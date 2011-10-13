package alg.cluster;

import io.SDFUtil;
import main.Settings;
import main.TaskProvider;
import data.ClusterDataImpl;
import data.DatasetFile;
import dataInterface.ClusterData;

public final class DatasetClustererUtil
{

	public static void storeClusters(String sdfFile, String clusterFilePrefix, String clusterAlgName,
			Iterable<ClusterData> clusters)
	{
		TaskProvider.task().verbose("Storing cluster results in files");

		int count = 0;
		for (ClusterData c : clusters)
		{
			if (c.getSize() == 0)
				throw new Error("try to store empty cluster");

			//String parent = origFile.substring(0, origFile.lastIndexOf("/"));
			//String origName = origFile.substring(origFile.lastIndexOf("/") + 1);

			String name = clusterFilePrefix + "_cluster_" + count++ + ".sdf";
			String clusterFile = Settings.destinationFile(sdfFile, name);

			// already loaded file may be overwritten, clear
			DatasetFile.clearFilesWith3DSDF(clusterFile);

			SDFUtil.filter(sdfFile, clusterFile, ((ClusterDataImpl) c).calculateCompoundIndices());
			((ClusterDataImpl) c).setName("Cluster " + count);
			((ClusterDataImpl) c).setFilename(clusterFile);
		}
		if (count == 0)
			throw new Error("clusterer returned no cluster");

	}
}
