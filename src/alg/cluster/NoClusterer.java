package alg.cluster;

import gui.Progressable;
import gui.property.Property;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import data.ClusterDataImpl;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;

public class NoClusterer implements DatasetClusterer
{

	@Override
	public Property[] getProperties()
	{
		return null;
	}

	@Override
	public void setProperties(Property[] properties)
	{
	}

	@Override
	public String getName()
	{
		return "No Dataset Clusterer";
	}

	@Override
	public String getDescription()
	{
		return "No clustering is performed. This embedds all structures directly into 3D-space without separating them into clusters. "
				+ "This is a valid option especially for smaller datasets (but can be used for any dataset size).";
	}

	@Override
	public String getPreconditionErrors()
	{
		return null;
	}

	ClusterDataImpl c;

	@Override
	public void clusterDataset(DatasetFile dataset, List<CompoundData> compounds, List<MoleculeProperty> features,
			Progressable progress)
	{
		c = new ClusterDataImpl();
		c.setFilename(dataset.getSDFPath(true));
		c.setName("Single cluster");
		c.setPosition(new Vector3f(0f, 0f, 0f));
		for (CompoundData compound : compounds)
			c.addCompound(compound);
	}

	@Override
	public List<ClusterData> getClusters()
	{
		List<ClusterData> l = new ArrayList<ClusterData>();
		l.add(c);
		return l;
	}

	@Override
	public boolean requiresNumericalFeatures()
	{
		return false;
	}

	@Override
	public String getFixedNumClustersProperty()
	{
		return null;
	}

}
