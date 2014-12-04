package org.chesmapper.map.alg.cluster;

import java.util.List;

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.alg.DistanceMeasure;
import org.chesmapper.map.alg.cluster.r.CascadeKMeansRClusterer;
import org.chesmapper.map.alg.cluster.r.DynamicTreeCutHierarchicalRClusterer;
import org.chesmapper.map.alg.cluster.r.HierarchicalRClusterer;
import org.chesmapper.map.alg.cluster.r.KMeansRClusterer;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.ClusterData;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.util.ArrayUtil;

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
