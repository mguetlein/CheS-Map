package alg.embed3d;

import java.util.List;

import javax.vecmath.Vector3f;

import main.Settings;
import alg.AbstractAlgorithm;
import alg.Message;
import alg.cluster.DatasetClusterer;
import data.DatasetFile;
import dataInterface.MoleculeProperty.Type;

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
	public Message getMessage(DatasetFile dataset, int numFeatures, Type featureType, boolean smartsFeaturesSelected,
			DatasetClusterer clusterer)
	{
		if (requiresFeatures() && numFeatures == 0)
			return Message.errorMessage(Settings.text("embed.error.no-features", getName()));
		else if (requiresFeatures() && featureType == Type.NOMINAL)
			return Message.infoMessage(Settings.text("embed.info.only-nominal"));
		return null;
	}
}
