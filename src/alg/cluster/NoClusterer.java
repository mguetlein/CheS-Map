package alg.cluster;

import gui.property.Property;

import java.util.ArrayList;
import java.util.List;

import main.Settings;
import alg.AbstractAlgorithm;
import data.ClusterDataImpl;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;

public class NoClusterer extends AbstractAlgorithm implements DatasetClusterer
{
	public static final NoClusterer INSTANCE = new NoClusterer();

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
	public void clusterDataset(DatasetFile dataset, List<CompoundData> compounds, List<MoleculeProperty> features)
	{
		clusters = new ArrayList<ClusterData>();
		ClusterDataImpl c = new ClusterDataImpl();
		c.setFilename(dataset.getSDFPath(true));
		c.setName("Single cluster");
		//		c.setPosition(new Vector3f(0f, 0f, 0f));
		for (CompoundData compound : compounds)
			c.addCompound(compound);
		clusters.add(c);
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
}
