package alg.embed3d;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Message;
import gui.Messages;

import java.io.File;
import java.util.HashMap;
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

	private HashMap<CorrelationType, Double> correlationValue = new HashMap<CorrelationType, Double>();
	private HashMap<CorrelationType, CompoundProperty> correlationProp = new HashMap<CorrelationType, CompoundProperty>();

	@Override
	public final List<Vector3f> getPositions()
	{
		return positions;
	}

	@Override
	public double getCorrelation(CorrelationType t)
	{
		if (correlationValue.containsKey(t))
			return correlationValue.get(t);
		else
			return Double.NaN;
	}

	@Override
	public CompoundProperty getCorrelationProperty(CorrelationType t)
	{
		return correlationProp.get(t);
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		if (requiresFeatures() && !featureInfo.featuresSelected)
			m.add(Message.errorMessage(Settings.text("error.no-features")));
		//		else if (requiresFeatures() && !featureInfo.numericFeaturesSelected
		//				&& (featureInfo.numFeatures < dataset.numCompounds() * 0.25))
		//			m.add(Message.infoMessage(Settings.text("embed.info.only-few-nominal")));
		return m;
	}

	protected abstract List<Vector3f> embed(DatasetFile dataset, List<CompoundData> instances,
			List<CompoundProperty> features) throws Exception; //boolean[] trainInstances

	protected abstract String getShortName();

	protected DatasetFile dataset;
	protected List<CompoundData> instances;
	protected List<CompoundProperty> features;
	protected DistanceMatrix dist;
	protected String distFilename;

	protected abstract boolean storesDistances();

	@Override
	public void embedDataset(DatasetFile dataset, List<CompoundData> instances, List<CompoundProperty> features)
			throws Exception //boolean[] trainInstances
	{
		this.dataset = dataset;
		this.features = features;
		this.instances = instances;
		dist = null;

		String embedFilename = dataset.getEmbeddingResultsFilePath("pos");
		HashMap<CorrelationType, String> corrValueFilenames = new HashMap<CorrelationType, String>();
		HashMap<CorrelationType, String> corrPropFilenames = new HashMap<CorrelationType, String>();
		HashMap<CorrelationType, double[]> corrPropValues = new HashMap<CorrelationType, double[]>();
		for (CorrelationType t : CorrelationType.types())
		{
			corrValueFilenames.put(t, dataset.getEmbeddingResultsFilePath(t.name().toLowerCase()));
			corrPropFilenames.put(t, dataset.getEmbeddingResultsFilePath(t.name().toLowerCase() + "Prop"));
		}
		distFilename = dataset.getEmbeddingResultsFilePath("dist");

		boolean filesFound = Settings.CACHING_ENABLED && new File(embedFilename).exists();
		if (storesDistances())
			filesFound &= new File(distFilename).exists();
		if (!Settings.BIG_DATA)
		{
			for (String f : corrValueFilenames.values())
				filesFound &= new File(f).exists();
			for (String f : corrPropFilenames.values())
				filesFound &= new File(f).exists();
		}
		if (filesFound)
		{
			Settings.LOGGER.info("Read cached embedding results from: " + embedFilename);
			positions = ValueFileCache.readCachePosition2(embedFilename, instances.size());
			if (!Settings.BIG_DATA)
			{
				for (CorrelationType t : CorrelationType.types())
				{
					String f = corrValueFilenames.get(t);
					Settings.LOGGER.info("Read cached embedding " + t.name() + " from: " + f);
					correlationValue.put(t, DoubleUtil.parseDouble(FileUtil.readStringFromFile(f)));

					f = corrPropFilenames.get(t);
					Settings.LOGGER.info("Read cached embedding " + t.name() + " property from: " + f);
					corrPropValues.put(t, ArrayUtil.toPrimitiveDoubleArray(ValueFileCache.readCacheDouble2(f)));
				}
				// compute, as this might take a few seconds on larger datasets, to have it available later
				if (getFeatureDistanceMatrix() == null)
					throw new IllegalStateException("no distance matrix");
			}
		}
		else
		{
			positions = embed(dataset, instances, features); //, trainInstances
			TaskProvider.debug("Store embedding results to: " + embedFilename);
			ValueFileCache.writeCachePosition2(embedFilename, positions);
			if (!Settings.BIG_DATA)
			{
				for (CorrelationType t : CorrelationType.types())
				{
					TaskProvider.debug("Compute " + t.name());
					correlationValue.put(t, EmbedUtil.computeCorrelation(t, positions, getFeatureDistanceMatrix()));
					TaskProvider.debug("Store embedding " + t.name() + " to: " + corrValueFilenames.get(t));
					FileUtil.writeStringToFile(corrValueFilenames.get(t), correlationValue.get(t) + "");

					corrPropValues.put(t, EmbedUtil.computeCorrelations(t, positions, getFeatureDistanceMatrix()));
					ValueFileCache.writeCacheDouble2(corrPropFilenames.get(t), ArrayUtil.toList(corrPropValues.get(t)));
				}
			}
		}
		if (!Settings.BIG_DATA)
		{
			for (CorrelationType t : CorrelationType.types())
				correlationProp.put(t, new CorrelationProperty(t, corrPropValues.get(t)));
		}

		if (positions.size() != instances.size())
			throw new IllegalStateException("illegal num positions " + positions.size() + " " + instances.size());
	}
}
