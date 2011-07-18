package alg.cluster;

import io.SDFUtil;
import main.Settings;
import data.CDKService;
import data.ClusterDataImpl;
import dataInterface.ClusterData;

public final class DatasetClustererUtil
{

	public static void storeClusters(String filename, String clusterFilePrefix, Iterable<ClusterData> clusters)
	{
		int count = 0;
		for (ClusterData c : clusters)
		{
			//String parent = origFile.substring(0, origFile.lastIndexOf("/"));
			//String origName = origFile.substring(origFile.lastIndexOf("/") + 1);

			String name = clusterFilePrefix + "_cluster_" + count++ + ".sdf";
			String clusterFile = Settings.destinationFile(filename, name);
			name = clusterFilePrefix + " Cluster " + count;

			CDKService.clear(clusterFile);
			SDFUtil.filter(filename, clusterFile, ((ClusterDataImpl) c).calculateCompoundIndices());
			((ClusterDataImpl) c).setName(name);
			((ClusterDataImpl) c).setFilename(clusterFile);
		}
	}
}
