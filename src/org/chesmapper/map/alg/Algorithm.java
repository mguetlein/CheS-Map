package org.chesmapper.map.alg;

import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.gui.FeatureWizardPanel.FeatureInfo;
import org.mg.javalib.gui.Messages;
import org.mg.javalib.gui.binloc.Binary;
import org.mg.javalib.gui.property.Property;

public interface Algorithm
{
	public Property[] getProperties();

	public String getName();

	public String getDescription();

	public Binary getBinary();

	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer);

	public Messages getProcessMessages();

	public Property getRandomSeedProperty();

	public Property getRandomRestartProperty();

	public void update(DatasetFile dataset);
}
