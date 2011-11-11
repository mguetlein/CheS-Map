package alg.align3d;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Message;
import gui.Messages;
import main.Settings;
import alg.AbstractAlgorithm;
import alg.cluster.DatasetClusterer;
import data.DatasetFile;

public abstract class Abstract3DAligner extends AbstractAlgorithm implements ThreeDAligner
{
	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		if (requiresStructuralFragments() && !featureInfo.smartsFeaturesSelected)
			m.add(Message.errorMessage(Settings.text("align.error.no-struct")));
		return m;
	}
}
