package alg.align3d;

import java.util.List;

import alg.Algorithm;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.CompoundProperty;
import dataInterface.SubstructureSmartsType;

public interface ThreeDAligner extends Algorithm
{
	public static final int MIN_NUM_ATOMS = 3;

	public static final ThreeDAligner ALIGNER[] = new ThreeDAligner[] { NoAligner.INSTANCE, MCSAligner.INSTANCE,
			MaxFragAligner.INSTANCE, ManualAligner.INSTANCE };

	public void algin(DatasetFile dataset, List<ClusterData> clusters, List<CompoundProperty> features);

	public String getAlginedClusterFile(int clusterIndex);

	public boolean requiresStructuralFragments();

	public SubstructureSmartsType getSubstructureSmartsType();
}
