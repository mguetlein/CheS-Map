package alg;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Messages;
import gui.binloc.Binary;
import gui.property.Property;
import alg.cluster.DatasetClusterer;
import data.DatasetFile;

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
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		return new Messages();
	}

	@Override
	public Property getRandomSeedProperty()
	{
		return null;
	}

	@Override
	public Property getRandomRestartProperty()
	{
		return null;
	}

	protected Messages processMessages = new Messages();

	@Override
	public Messages getProcessMessages()
	{
		return processMessages;
	}

	@Override
	public void update(DatasetFile dataset)
	{
	}

}
