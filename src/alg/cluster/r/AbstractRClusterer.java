package alg.cluster.r;

import gui.binloc.Binary;
import gui.property.PropertyUtil;
import io.RUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import data.DatasetFile;
import data.DistanceUtil;
import dataInterface.CompoundData;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculePropertyUtil;

public abstract class AbstractRClusterer extends AbstractDatasetClusterer
{
	public static final DatasetClusterer[] R_CLUSTERER = new AbstractRClusterer[] { new KMeansRClusterer(),
			new CascadeKMeansRClusterer(), new HierarchicalRClusterer(), new DynamicTreeCutHierarchicalRClusterer() };

	// new ModelBasedRClusterer(),

	@Override
	public Binary getBinary()
	{
		return Settings.RSCRIPT_BINARY;
	}

	protected abstract String getRScriptCode();

	public static String TOO_FEW_UNIQUE_DATA_POINTS = "Too few unique data points, add features or decrease number of clusters.";

	@Override
	protected List<Integer> cluster(DatasetFile dataset, List<CompoundData> compounds, List<MoleculeProperty> features)
			throws IOException
	{
		File tmp = File.createTempFile(dataset.getShortName(), "cluster");
		try
		{
			String propsMD5 = PropertyUtil.getPropertyMD5(getProperties());
			String datasetMD5 = MoleculePropertyUtil.getSetMD5(features, dataset.getMD5());

			String featureTableFile = Settings.destinationFile(dataset, dataset.getShortName() + "." + datasetMD5
					+ ".features.table");
			if (!new File(featureTableFile).exists())
				ExportRUtil.toRTable(features,
						DistanceUtil.values(features, ListUtil.cast(MolecularPropertyOwner.class, compounds)),
						featureTableFile);
			else
				System.out.println("load cached features from " + featureTableFile);
			String errorOut = ExternalToolUtil.run(
					getShortName(),
					new String[] {
							Settings.RSCRIPT_BINARY.getLocation(),
							FileUtil.getAbsolutePathEscaped(new File(RScriptUtil.getScriptPath(getShortName()
									+ propsMD5, getRScriptCode()))),
							FileUtil.getAbsolutePathEscaped(new File(featureTableFile)),
							FileUtil.getAbsolutePathEscaped(tmp) });

			List<Integer> cluster = new ArrayList<Integer>();
			if (tmp.exists())
				cluster = RUtil.readCluster(tmp.getAbsolutePath());
			if (cluster.size() != compounds.size())
			{
				if (errorOut.contains(TOO_FEW_UNIQUE_DATA_POINTS))
					throw new ClusterException(this, getShortName() + " failed: " + TOO_FEW_UNIQUE_DATA_POINTS);
				// else: unknown exception
				throw new IllegalStateException(getShortName() + " failed: \n" + errorOut);
			}
			return cluster;
		}
		finally
		{
			tmp.delete();
		}
	}
}
