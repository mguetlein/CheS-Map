package alg.cluster.r;

import gui.binloc.Binary;
import io.RUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.Settings;
import rscript.ExportRUtil;
import rscript.RScriptUtil;
import util.ExternalToolUtil;
import util.ListUtil;
import alg.cluster.AbstractDatasetClusterer;
import alg.cluster.DatasetClusterer;
import data.ClusterDataImpl;
import data.DatasetFile;
import data.DistanceUtil;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

public abstract class AbstractRClusterer extends AbstractDatasetClusterer
{
	public static final DatasetClusterer[] R_CLUSTERER = new AbstractRClusterer[] { new KMeansRClusterer(),
			new CascadeKMeansRClusterer(), new HierarchicalRClusterer(), new DynamicTreeCutHierarchicalRClusterer() };

	//new ModelBasedRClusterer(), 

	@Override
	public Binary getBinary()
	{
		return Settings.RSCRIPT_BINARY;
	}

	protected abstract String getRScriptName();

	protected abstract String getRScriptCode();

	@Override
	public void clusterDataset(DatasetFile dataset, List<CompoundData> compounds, List<MoleculeProperty> features)
	{
		File f = null;
		File f2 = null;
		try
		{
			f = File.createTempFile("features", ".table");
			f2 = File.createTempFile("cluster", ".matrix");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		ExportRUtil.toRTable(features,
				DistanceUtil.values(features, ListUtil.cast(MolecularPropertyOwner.class, compounds)),
				f.getAbsolutePath());

		String errorOut = ExternalToolUtil.run(
				getRScriptName(),
				Settings.RSCRIPT_BINARY.getLocation() + " "
						+ RScriptUtil.getScriptPath(getRScriptName(), getRScriptCode()) + " " + f.getAbsolutePath()
						+ " " + f2.getAbsolutePath());

		List<Integer> cluster = RUtil.readCluster(f2.getAbsolutePath());
		if (cluster.size() != compounds.size())
			throw new Error(getRScriptName() + " failed: \n" + errorOut);
		//			throw new IllegalStateException("error using '" + getRScriptName() + "' num results is '" + cluster.size()
		//					+ "' instead of '" + compounds.size() + "'");

		clusters = new ArrayList<ClusterData>();
		HashMap<Integer, ClusterDataImpl> map = new HashMap<Integer, ClusterDataImpl>();
		for (int i = 0; i < cluster.size(); i++)
		{
			if (!map.containsKey(cluster.get(i)))
			{
				ClusterDataImpl c = new ClusterDataImpl();
				clusters.add(c);
				map.put(cluster.get(i), c);

			}
			ClusterDataImpl c = map.get(cluster.get(i));
			c.addCompound(compounds.get(i));
		}

		storeClusters(dataset.getSDFPath(true), getRScriptName(), getName(), clusters);
	}

}
