package alg.cluster.r;

import gui.binloc.Binary;
import io.RUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.Settings;
import rscript.ExportRUtil;
import rscript.RScriptUtil;
import util.ExternalToolUtil;
import util.FileUtil;
import util.ListUtil;
import alg.AlgorithmException.ClusterException;
import alg.cluster.AbstractDatasetClusterer;
import alg.cluster.DatasetClusterer;
import data.ClusterDataImpl;
import data.DatasetFile;
import data.DistanceUtil;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculePropertyUtil;

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

	public static String TOO_FEW_UNIQUE_DATA_POINTS = "Too few unique data points, add features or decrease number of clusters.";

	@Override
	public void clusterDataset(DatasetFile dataset, List<CompoundData> compounds, List<MoleculeProperty> features)
	{
		// name is used : encode dataset md5 + feature md5 into name!
		String enc = MoleculePropertyUtil.getSetMD5(features, dataset.getMD5());
		String clusterMatrixFile = Settings.destinationFile(dataset.getSDFPath(false), getRScriptName() + "."
				+ FileUtil.getFilename(dataset.getSDFPath(false)) + "." + enc + ".cluster.matrix");

		String errorOut = null;
		if (!new File(clusterMatrixFile).exists())
		{
			String featureTableFile = Settings.destinationFile(dataset.getSDFPath(false),
					FileUtil.getFilename(dataset.getSDFPath(false)) + "." + enc + ".features.table");
			if (!new File(featureTableFile).exists())
				ExportRUtil.toRTable(features,
						DistanceUtil.values(features, ListUtil.cast(MolecularPropertyOwner.class, compounds)),
						featureTableFile);
			else
				System.out.println("load cached features from " + featureTableFile);
			errorOut = ExternalToolUtil.run(
					getRScriptName(),
					Settings.RSCRIPT_BINARY.getLocation() + " "
							+ RScriptUtil.getScriptPath(getRScriptName(), getRScriptCode()) + " " + featureTableFile
							+ " " + clusterMatrixFile);
		}
		else
			System.out.println("load cached clustering from " + clusterMatrixFile);

		List<Integer> cluster = new ArrayList<Integer>();
		if (new File(clusterMatrixFile).exists())
			cluster = RUtil.readCluster(clusterMatrixFile);
		if (cluster.size() != compounds.size())
		{
			if (errorOut.contains(TOO_FEW_UNIQUE_DATA_POINTS))
				throw new ClusterException(this, getRScriptName() + " failed: " + TOO_FEW_UNIQUE_DATA_POINTS);
			// else: unknown exception
			throw new IllegalStateException(getRScriptName() + " failed: \n" + errorOut);
		}
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
