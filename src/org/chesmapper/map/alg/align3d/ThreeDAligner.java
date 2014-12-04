package org.chesmapper.map.alg.align3d;

import java.util.List;

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.ClusterData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.SubstructureSmartsType;

public interface ThreeDAligner extends Algorithm
{
	public static final int MIN_NUM_ATOMS = 3;

	public static final ThreeDAligner ALIGNER[] = new ThreeDAligner[] { NoAligner.INSTANCE, MCSAligner.INSTANCE,
			MaxFragAligner.INSTANCE, ManualAligner.INSTANCE };

	public void algin(DatasetFile dataset, List<ClusterData> clusters, List<CompoundProperty> features);

	public String getAlignedSDFile();

	public boolean requiresStructuralFragments();

	public SubstructureSmartsType getSubstructureSmartsType();
}
