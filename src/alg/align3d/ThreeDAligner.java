package alg.align3d;

import java.util.List;

import alg.Algorithm;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.MoleculeProperty;
import dataInterface.SubstructureSmartsType;

public interface ThreeDAligner extends Algorithm
{
	public void algin(DatasetFile dataset, List<ClusterData> clusters, List<MoleculeProperty> features);

	public List<String> getAlginedClusterFiles();

	public boolean requiresStructuralFragments();

	public SubstructureSmartsType getSubstructureSmartsType();
}
