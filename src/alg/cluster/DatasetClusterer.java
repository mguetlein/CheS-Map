package alg.cluster;

import gui.property.Property;

import java.util.List;

import util.ArrayUtil;
import alg.Algorithm;
import alg.cluster.r.AbstractRClusterer;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;

public interface DatasetClusterer extends Algorithm
{
	public static DatasetClusterer CLUSTERERS[] = ArrayUtil.concat(DatasetClusterer.class,
			new DatasetClusterer[] { NoClusterer.INSTANCE }, WekaClusterer.WEKA_CLUSTERER,
			AbstractRClusterer.R_CLUSTERER);

	public void clusterDataset(DatasetFile dataset, List<CompoundData> compounds, List<MoleculeProperty> features)
			throws Exception;

	public List<ClusterData> getClusters();

	public boolean requiresFeatures();

	public Property getFixedNumClustersProperty();

	public ClusterApproach getClusterApproach();

	public Property getDistanceFunctionProperty();
}
