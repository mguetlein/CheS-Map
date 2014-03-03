package alg.embed3d;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Message;
import gui.Messages;

import java.io.File;
import java.util.List;

import javax.vecmath.Vector3f;

import main.Settings;
import main.TaskProvider;
import util.ArrayUtil;
import util.DoubleUtil;
import util.FileUtil;
import util.ValueFileCache;
import alg.AbstractAlgorithm;
import alg.cluster.DatasetClusterer;
import data.DatasetFile;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;

public abstract class Abstract3DEmbedder extends AbstractAlgorithm implements ThreeDEmbedder
{
	@Override
	public boolean requiresFeatures()
	{
		return true;
	}

	protected List<Vector3f> positions;
	private double rSquare = -Double.MAX_VALUE;
	private double ccc = -Double.MAX_VALUE;

	private CompoundProperty cccProp;

	@Override
	public final List<Vector3f> getPositions()
	{
		return positions;
	}

	//	@Override
	//	public double getRSquare()
	//	{
	//		return rSquare;
	//	}

	@Override
	public double getCCC()
	{
		return ccc;
	}

	@Override
	public CompoundProperty getCCCProperty()
	{
		return cccProp;
	}

	//	@Override
	//	public CompoundPropertyEmbedQuality getEmbedQuality(CompoundProperty p, DatasetFile dataset,
	//			List<CompoundPropertyOwner> instances)
	//	{
	//		return new CompoundPropertyEmbedQuality(p, positions, instances, dataset);
	//	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		if (requiresFeatures() && !featureInfo.featuresSelected)
			m.add(Message.errorMessage(Settings.text("error.no-features")));
		else if (requiresFeatures() && !featureInfo.numericFeaturesSelected
				&& (featureInfo.numFeatures < dataset.numCompounds() * 0.25))
			m.add(Message.infoMessage(Settings.text("embed.info.only-few-nominal")));
		return m;
	}

	protected abstract List<Vector3f> embed(DatasetFile dataset, List<CompoundData> instances,
			List<CompoundProperty> features) throws Exception; //boolean[] trainInstances

	protected abstract String getShortName();

	protected abstract boolean storesDistances();

	protected DatasetFile dataset;
	protected List<CompoundData> instances;
	protected List<CompoundProperty> features;
	protected DistanceMatrix dist;

	@Override
	public void embedDataset(DatasetFile dataset, List<CompoundData> instances, List<CompoundProperty> features)
			throws Exception //boolean[] trainInstances
	{
		this.dataset = dataset;
		this.features = features;
		this.instances = instances;
		dist = null;

		String embedFilename = dataset.getEmbeddingResultsFilePath("pos");
		//String rSquareFilename = dataset.getEmbeddingResultsFilePath("rSquare");
		String cccFilename = dataset.getEmbeddingResultsFilePath("ccc");
		String cccPropFilename = dataset.getEmbeddingResultsFilePath("cccProp");

		String distFilename = dataset.getEmbeddingResultsFilePath("dist");

		double cccPropValues[];

		if (Settings.CACHING_ENABLED
				&& new File(embedFilename).exists() //&& new File(rSquareFilename).exists()
				&& new File(cccFilename).exists() && new File(cccPropFilename).exists()
				&& (!storesDistances() || new File(distFilename).exists()))
		{
			Settings.LOGGER.info("Read cached embedding results from: " + embedFilename);
			positions = ValueFileCache.readCachePosition2(embedFilename, instances.size());
			//			Settings.LOGGER.info("Read cached embedding rSquare from: " + rSquareFilename);
			//			rSquare = DoubleUtil.parseDouble(FileUtil.readStringFromFile(rSquareFilename));
			Settings.LOGGER.info("Read cached embedding ccc from: " + cccFilename);
			ccc = DoubleUtil.parseDouble(FileUtil.readStringFromFile(cccFilename));
			Settings.LOGGER.info("Read cached embedding ccc property from: " + cccPropFilename);
			cccPropValues = ArrayUtil.toPrimitiveDoubleArray(ValueFileCache.readCacheDouble2(cccPropFilename));
		}
		else
		{
			positions = embed(dataset, instances, features); //, trainInstances

			TaskProvider.debug("Store embedding results to: " + embedFilename);
			ValueFileCache.writeCachePosition2(embedFilename, positions);

			TaskProvider.debug("Compute rSquare");

			//			rSquare = EmbedUtil.computeRSquare(positions, getFeatureDistanceMatrix());
			//			TaskProvider.debug("Store embedding rSquare to: " + rSquareFilename);
			//			FileUtil.writeStringToFile(rSquareFilename, rSquare + "");

			TaskProvider.debug("Compute ccc");
			ccc = EmbedUtil.computeCCC(positions, getFeatureDistanceMatrix());
			//				ccc = EmbedUtil.computeCCC(positions, instances, features, dataset);
			TaskProvider.debug("Store embedding ccc to: " + cccFilename);
			FileUtil.writeStringToFile(cccFilename, ccc + "");

			cccPropValues = EmbedUtil.computeCCCs(positions, getFeatureDistanceMatrix());
			//			cccPropValues = EmbedUtil.computeCCCs(positions, instances, features, dataset);
			ValueFileCache.writeCacheDouble2(cccPropFilename, ArrayUtil.toList(cccPropValues));
		}
		cccProp = CCCPropertySet.create(dataset, cccPropValues, embedFilename);

		if (positions.size() != instances.size())
			throw new IllegalStateException("illegal num positions " + positions.size() + " " + instances.size());
	}
}
