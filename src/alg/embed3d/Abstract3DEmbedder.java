package alg.embed3d;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Message;
import gui.Messages;
import gui.property.PropertyUtil;

import java.io.File;
import java.util.List;

import javax.vecmath.Vector3f;

import main.Settings;
import util.DoubleUtil;
import util.FileUtil;
import util.ValueFileCache;
import alg.AbstractAlgorithm;
import alg.cluster.DatasetClusterer;
import data.DatasetFile;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculePropertyUtil;

public abstract class Abstract3DEmbedder extends AbstractAlgorithm implements ThreeDEmbedder
{
	@Override
	public boolean requiresFeatures()
	{
		return true;
	}

	private List<Vector3f> positions;
	protected double rSquare = -Double.MAX_VALUE;

	@Override
	public final List<Vector3f> getPositions()
	{
		return positions;
	}

	@Override
	public double getRSquare()
	{
		if (rSquare == -Double.MAX_VALUE)
			throw new IllegalStateException("compute r-square first");
		return rSquare;
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

	protected abstract List<Vector3f> embed(DatasetFile dataset, List<MolecularPropertyOwner> instances,
			List<MoleculeProperty> features) throws Exception;

	protected abstract String getShortName();

	public void embedDataset(DatasetFile dataset, List<MolecularPropertyOwner> instances,
			List<MoleculeProperty> features) throws Exception
	{
		String basename = dataset.getShortName()
				+ "."
				+ getShortName()
				+ "."
				+ MoleculePropertyUtil.getSetMD5(features,
						dataset.getMD5() + " " + PropertyUtil.getPropertyMD5(getProperties()));
		String embedFilename = Settings.destinationFile(dataset, basename + ".embed");
		String rSquareFilename = Settings.destinationFile(dataset, basename + ".rSquare");

		if (Settings.CACHING_ENABLED && new File(embedFilename).exists() && new File(rSquareFilename).exists())
		{
			Settings.LOGGER.info("read cached embedding results from: " + embedFilename);
			positions = ValueFileCache.readCachePosition2(embedFilename, instances.size());
			Settings.LOGGER.info("read cached embedding rSquare from: " + rSquareFilename);
			rSquare = DoubleUtil.parseDouble(FileUtil.readStringFromFile(rSquareFilename));
		}
		else
		{
			positions = embed(dataset, instances, features);
			Settings.LOGGER.info("store embedding results to: " + embedFilename);
			ValueFileCache.writeCachePosition2(embedFilename, positions);
			Settings.LOGGER.info("store embedding rSquare to: " + rSquareFilename);
			FileUtil.writeStringToFile(rSquareFilename, rSquare + "");
		}

		if (positions.size() != instances.size())
			throw new IllegalStateException("illegal num positions " + positions.size() + " " + instances.size());
	}
}
