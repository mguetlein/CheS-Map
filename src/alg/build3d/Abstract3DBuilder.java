package alg.build3d;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Message;
import gui.Messages;
import main.Settings;
import alg.AbstractAlgorithm;
import alg.cluster.DatasetClusterer;
import data.DatasetFile;

public abstract class Abstract3DBuilder extends AbstractAlgorithm implements ThreeDBuilder
{
	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		if (isReal3DBuilder() && isCached(dataset))
			m.add(Message.infoMessage(Settings.text("build3d.info.cached", getName())));
		else if (dataset.has3D() && isReal3DBuilder())
			m.add(Message.warningMessage(Settings.text("build3d.warn.already-3d", dataset.getName())));
		else if (!dataset.has3D() && !isReal3DBuilder())
			m.add(Message.warningMessage(Settings.text("build3d.warn.3d-missing", dataset.getName())));
		else if (isReal3DBuilder() && !isCached(dataset))
			m.add(Message.slowMessage(Settings.text("build3d.slow")));
		return m;
	}
}
