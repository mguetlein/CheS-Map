package alg.build3d;

import main.Settings;
import alg.AbstractAlgorithm;
import alg.Message;
import alg.cluster.DatasetClusterer;
import data.DatasetFile;
import dataInterface.MoleculeProperty.Type;

public abstract class Abstract3DBuilder extends AbstractAlgorithm implements ThreeDBuilder
{
	@Override
	public Message getMessage(DatasetFile dataset, int numFeatures, Type featureType, boolean smartsFeaturesSelected,
			DatasetClusterer clusterer)
	{
		if (isReal3DBuilder() && threeDFileAlreadyExists(dataset))
			return Message.infoMessage(Settings.text("build3d.info.cached", getName()));
		else if (dataset.has3D() && isReal3DBuilder())
			return Message.warningMessage(Settings.text("build3d.warn.already-3d", dataset.getName()));
		else if (!dataset.has3D() && !isReal3DBuilder())
			return Message.warningMessage(Settings.text("build3d.warn.3d-missing", dataset.getName()));
		return null;
	}
}
