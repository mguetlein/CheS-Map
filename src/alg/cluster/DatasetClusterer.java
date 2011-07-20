package alg.cluster;

import gui.Progressable;

import java.util.List;

import alg.Algorithm;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;

public interface DatasetClusterer extends Algorithm
{
	public void clusterDataset(DatasetFile dataset, List<CompoundData> compounds, List<MoleculeProperty> features,
			Progressable progress);

	public List<ClusterData> getClusters();

	public boolean requiresNumericalFeatures();
}
