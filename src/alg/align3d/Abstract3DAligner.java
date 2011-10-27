package alg.align3d;

import main.Settings;
import alg.AbstractAlgorithm;
import alg.Message;
import alg.cluster.DatasetClusterer;
import data.DatasetFile;
import dataInterface.MoleculeProperty.Type;

public abstract class Abstract3DAligner extends AbstractAlgorithm implements ThreeDAligner
{
	@Override
	public Message getMessage(DatasetFile dataset, int numFeatures, Type featureType, boolean smartsFeaturesSelected,
			DatasetClusterer clusterer)
	{
		if (requiresStructuralFragments() && !smartsFeaturesSelected)
			return Message.errorMessage(Settings.text("align.error.no-struct"));
		else
			return null;
	}
}
