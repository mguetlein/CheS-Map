package org.chesmapper.map.alg;

import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.gui.FeatureWizardPanel.FeatureInfo;
import org.mg.javalib.gui.Messages;
import org.mg.javalib.gui.binloc.Binary;
import org.mg.javalib.gui.property.Property;

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
