package alg;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Messages;
import gui.binloc.Binary;
import gui.property.Property;
import alg.cluster.DatasetClusterer;
import data.DatasetFile;

public interface Algorithm
{
	public Property[] getProperties();

	public String getName();

	public String getDescription();

	public Binary getBinary();

	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer);

	public Property getRandomSeedProperty();

	public Property getRandomRestartProperty();
}
