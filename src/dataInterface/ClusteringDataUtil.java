package dataInterface;

import javax.vecmath.Vector3f;

import data.ClusteringData;

public class ClusteringDataUtil
{
	public static Vector3f[] getClusterPositions(ClusteringData d)
	{
		Vector3f list[] = new Vector3f[d.getSize()];
		int i = 0;
		for (ClusterData c : d.getClusters())
			list[i++] = c.getPosition();
		return list;
	}

	public static Vector3f[] getCompoundPositions(ClusterData d)
	{
		Vector3f list[] = new Vector3f[d.getSize()];
		int i = 0;
		for (CompoundData c : d.getCompounds())
			list[i++] = c.getPosition();
		return list;
	}
}
