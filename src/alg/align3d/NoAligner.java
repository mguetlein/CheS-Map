package alg.align3d;

import gui.property.Property;

import java.util.ArrayList;
import java.util.List;

import data.DatasetFile;
import dataInterface.ClusterData;

public class NoAligner implements ThreeDAligner
{
	List<String> clusterFiles;

	@Override
	public String getDescription()
	{
		return "Does NOT align compounds, leaves the dataset as it is.";
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
			clusterFiles.add(c.getFilename());
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
	public String getPreconditionErrors()
	{
		return null;
	}
}
