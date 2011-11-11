package alg.embed3d;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Message;
import gui.Messages;

import java.util.List;

import javax.vecmath.Vector3f;

import main.Settings;
import alg.AbstractAlgorithm;
import alg.cluster.DatasetClusterer;
import data.DatasetFile;

public abstract class Abstract3DEmbedder extends AbstractAlgorithm implements ThreeDEmbedder
{
	@Override
	public boolean requiresFeatures()
	{
		return true;
	}

	protected List<Vector3f> positions;

	@Override
	public final List<Vector3f> getPositions()
	{
		return positions;
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		if (requiresFeatures() && !featureInfo.featuresSelected)
			m.add(Message.errorMessage(Settings.text("error.no-features")));
		else if (requiresFeatures() && !featureInfo.numericFeaturesSelected)
			m.add(Message.infoMessage(Settings.text("embed.info.only-nominal")));
		return m;
	}
}
