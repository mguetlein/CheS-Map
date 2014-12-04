package org.chesmapper.map.alg.cluster;

import java.util.ArrayList;
import java.util.List;

import org.chesmapper.map.alg.AbstractAlgorithm;
import org.chesmapper.map.alg.DistanceMeasure;
import org.chesmapper.map.data.ClusterDataImpl;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.ClusterData;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.main.Settings;
import org.mg.javalib.gui.property.Property;

public class NoClusterer extends AbstractAlgorithm implements DatasetClusterer
{
	public static final NoClusterer INSTANCE = new NoClusterer();
	private String sdf;

	private NoClusterer()
	{
	}

	private List<ClusterData> clusters;

	@Override
	public String getName()
	{
		return getNameStatic();
	}

	public static String getNameStatic()
	{
		return Settings.text("cluster.no-cluster");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("cluster.no-cluster.desc");
	}

	@Override
	public void clusterDataset(DatasetFile dataset, List<CompoundData> compounds, List<CompoundProperty> features)
	{
		sdf = dataset.getSDF3D();
		clusters = new ArrayList<ClusterData>();
		ClusterDataImpl c = new ClusterDataImpl();
		c.setName("Single cluster");
		c.setOrigIndex(0);
		for (CompoundData compound : compounds)
			c.addCompound(compound);
		clusters.add(c);

		List<Integer> clusterIdx = new ArrayList<Integer>();
		for (CompoundData comp : c.getCompounds())
			clusterIdx.add(comp.getOrigIndex());
		c.setCompoundClusterIndices(clusterIdx);
	}

	@Override
	public String getClusterSDFile()
	{
		return sdf;
	}

	@Override
	public boolean requiresFeatures()
	{
		return false;
	}

	@Override
	public List<ClusterData> getClusters()
	{
		return clusters;
	}

	@Override
	public Property getFixedNumClustersProperty()
	{
		return null;
	}

	@Override
	public ClusterApproach getClusterApproach()
	{
		return ClusterApproach.Other;
	}

	@Override
	public Property getDistanceFunctionProperty()
	{
		return null;
	}

	@Override
	public boolean isDisjointClusterer()
	{
		return true;
	}

	@Override
	public DistanceMeasure getDistanceMeasure()
	{
		return DistanceMeasure.UNKNOWN_DISTANCE;
	}
}
