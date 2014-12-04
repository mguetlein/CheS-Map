package org.chesmapper.map.alg.cluster.r;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.chesmapper.map.alg.AlgorithmException.ClusterException;
import org.chesmapper.map.alg.cluster.AbstractDatasetClusterer;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertyUtil;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.chesmapper.map.rscript.ExportRUtil;
import org.chesmapper.map.util.ExternalToolUtil;
import org.mg.javalib.gui.binloc.Binary;
import org.mg.javalib.gui.property.PropertyUtil;
import org.mg.javalib.io.RUtil;
import org.mg.javalib.util.FileUtil;

public abstract class AbstractRClusterer extends AbstractDatasetClusterer
{
	@Override
	public Binary getBinary()
	{
		return BinHandler.RSCRIPT_BINARY;
	}

	protected abstract String getRScriptCode();

	public static String TOO_FEW_UNIQUE_DATA_POINTS = "Too few unique data points, add features or decrease number of clusters.";

	@Override
	protected List<Integer[]> cluster(DatasetFile dataset, List<CompoundData> compounds, List<CompoundProperty> features)
			throws IOException
	{
		File tmp = File.createTempFile(dataset.getShortName(), "cluster");
		File rScript = null;
		try
		{
			String featureTableFile = dataset.getFeatureTableFilePath("table");
			if (!Settings.CACHING_ENABLED || !new File(featureTableFile).exists())
				ExportRUtil.toRTable(features,
						CompoundPropertyUtil.valuesReplaceNullWithMedian(features, compounds, dataset),
						featureTableFile);
			else
				Settings.LOGGER.info("load cached features from " + featureTableFile);

			Settings.LOGGER.info("Using r-clusterer " + getName() + " with properties: "
					+ PropertyUtil.toString(getProperties()));

			rScript = File.createTempFile("rscript", "R");
			FileUtil.writeStringToFile(rScript.getAbsolutePath(), getRScriptCode());

			String errorOut = ExternalToolUtil.run(
					getShortName(),
					new String[] { BinHandler.RSCRIPT_BINARY.getLocation(), FileUtil.getAbsolutePathEscaped(rScript),
							FileUtil.getAbsolutePathEscaped(new File(featureTableFile)),
							FileUtil.getAbsolutePathEscaped(tmp) });
			if (!TaskProvider.isRunning())
				return null;

			List<Integer[]> cluster = new ArrayList<Integer[]>();
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
			if (rScript != null)
				rScript.delete();
		}
	}
}
