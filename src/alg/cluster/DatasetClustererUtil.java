package alg.cluster;

import io.SDFUtil;
import main.Settings;
import data.ClusterDataImpl;
import data.DatasetFile;
import dataInterface.ClusterData;

public final class DatasetClustererUtil
{

	public static void storeClusters(String sdfFile, String clusterFilePrefix, Iterable<ClusterData> clusters)
	{
		int count = 0;
		for (ClusterData c : clusters)
		{
			//String parent = origFile.substring(0, origFile.lastIndexOf("/"));
			//String origName = origFile.substring(origFile.lastIndexOf("/") + 1);

			String name = clusterFilePrefix + "_cluster_" + count++ + ".sdf";
			String clusterFile = Settings.destinationFile(sdfFile, name);
			name = clusterFilePrefix + " Cluster " + count;

			// already loaded file may be overwritten, clear
			DatasetFile.clearFilesWithSDF(clusterFile);

			SDFUtil.filter(sdfFile, clusterFile, ((ClusterDataImpl) c).calculateCompoundIndices());
			((ClusterDataImpl) c).setName(name);
			((ClusterDataImpl) c).setFilename(clusterFile);
		}
	}
}
