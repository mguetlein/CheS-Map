package alg.align3d;

import java.util.List;

import alg.Algorithm;
import dataInterface.ClusterData;

public interface ThreeDAligner extends Algorithm
{
	public void algin(List<ClusterData> clusters);

	public List<String> getAlginedClusterFiles();

	public boolean isRealAligner();
}
