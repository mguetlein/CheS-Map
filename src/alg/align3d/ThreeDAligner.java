package alg.align3d;

import java.util.List;

import alg.Algorithm;
import data.DatasetFile;
import dataInterface.ClusterData;

public interface ThreeDAligner extends Algorithm
{
	public void algin(DatasetFile dataset, List<ClusterData> clusters);

	public List<String> getAlginedClusterFiles();

	public boolean isRealAligner();
}
