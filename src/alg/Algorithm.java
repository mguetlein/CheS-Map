package alg;

import gui.binloc.Binary;
import gui.property.Property;
import alg.cluster.DatasetClusterer;
import data.DatasetFile;
import dataInterface.MoleculeProperty.Type;

public interface Algorithm
{
	public Property[] getProperties();

	public String getName();

	public String getDescription();

	public Binary getBinary();

	public Message getMessage(DatasetFile dataset, int numFeatures, Type featureType, boolean smartsFeaturesSelected,
			DatasetClusterer clusterer);
}
