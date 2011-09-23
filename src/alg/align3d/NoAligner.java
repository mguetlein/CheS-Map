package alg.align3d;

import gui.binloc.Binary;
import gui.property.Property;

import java.util.ArrayList;
import java.util.List;

import data.ClusterDataImpl;
import data.DatasetFile;
import dataInterface.ClusterData;

public class NoAligner implements ThreeDAligner
{
	List<String> clusterFiles;

	@Override
	public String getDescription()
	{
		return "Does NOT align compounds. Hence, the compounds are oriented in 3D space as provided in the dataset.";
	}

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
	public void algin(DatasetFile dataset, List<ClusterData> clusters)
	{
		clusterFiles = new ArrayList<String>();
		for (ClusterData c : clusters)
		{
			clusterFiles.add(c.getFilename());
			((ClusterDataImpl) c).setAlignAlgorithm(getName());
		}
	}

	@Override
	public List<String> getAlginedClusterFiles()
	{
		return clusterFiles;
	}

	@Override
	public String getName()
	{
		return "No Cluster Aligner";
	}

	@Override
	public Binary getBinary()
	{
		return null;
	}

	@Override
	public String getWarning()
	{
		return null;
	}
}
