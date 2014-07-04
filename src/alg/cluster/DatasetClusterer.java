package alg.cluster;

import gui.property.Property;

import java.util.List;

import util.ArrayUtil;
import alg.Algorithm;
import alg.DistanceMeasure;
import alg.cluster.r.CascadeKMeansRClusterer;
import alg.cluster.r.DynamicTreeCutHierarchicalRClusterer;
import alg.cluster.r.HierarchicalRClusterer;
import alg.cluster.r.KMeansRClusterer;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;

public interface DatasetClusterer extends Algorithm
{
	public static DatasetClusterer CLUSTERERS[] = ArrayUtil.concat(DatasetClusterer.class,
			new DatasetClusterer[] { NoClusterer.INSTANCE }, WekaClusterer.WEKA_CLUSTERER, new DatasetClusterer[] {
					KMeansRClusterer.INSTANCE, CascadeKMeansRClusterer.INSTANCE, HierarchicalRClusterer.INSTANCE,
					DynamicTreeCutHierarchicalRClusterer.INSTANCE },
			new DatasetClusterer[] { ManualClusterer.INSTANCE });

	public void clusterDataset(DatasetFile dataset, List<CompoundData> compounds, List<CompoundProperty> features)
			throws Exception;

	public List<ClusterData> getClusters();

	public String getClusterSDFile();

	public boolean isDisjointClusterer();

	public boolean requiresFeatures();

	public Property getFixedNumClustersProperty();

	public ClusterApproach getClusterApproach();

	public Property getDistanceFunctionProperty();

	public DistanceMeasure getDistanceMeasure();
}
