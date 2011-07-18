package alg.cluster;

import java.util.List;

import alg.Algorithm;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;

public interface DatasetClusterer extends Algorithm
{
	public void clusterDataset(String datasetName, String filename, List<CompoundData> compounds,
			List<MoleculeProperty> features);

	public List<ClusterData> getClusters();

	public boolean requiresNumericalFeatures();
}
