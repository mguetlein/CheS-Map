package alg.cluster;

import gui.property.Property;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import data.ClusterDataImpl;
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
		return "Does not perform clustering, i.e. a single cluster is returned.";
	}

	@Override
	public String getPreconditionErrors()
	{
		return null;
	}

	ClusterDataImpl c;

	@Override
	public void clusterDataset(String datasetName, String filename, List<CompoundData> compounds,
			List<MoleculeProperty> features)
	{
		c = new ClusterDataImpl();
		c.setFilename(filename);
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

}
