package alg;

import gui.binloc.Binary;
import gui.property.Property;
import alg.cluster.DatasetClusterer;
import data.DatasetFile;
import dataInterface.MoleculeProperty.Type;

public abstract class AbstractAlgorithm implements Algorithm
{

	@Override
	public Property[] getProperties()
	{
		return null;
	}

	@Override
	public Binary getBinary()
	{
		return null;
	}

	@Override
	public Message getMessage(DatasetFile dataset, int numFeatures, Type featureType, boolean smartsFeaturesSelected,
			DatasetClusterer clusterer)
	{
		return null;
	}

}
