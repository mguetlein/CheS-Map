package org.chesmapper.map.main;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import org.chesmapper.map.alg.FeatureComputer;
import org.chesmapper.map.alg.align3d.BigDataFakeAligner;
import org.chesmapper.map.alg.align3d.NoAligner;
import org.chesmapper.map.alg.align3d.ThreeDAligner;
import org.chesmapper.map.alg.build3d.ThreeDBuilder;
import org.chesmapper.map.alg.build3d.UseOrigStructures;
import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.alg.cluster.NoClusterer;
import org.chesmapper.map.alg.embed3d.CorrelationProperty;
import org.chesmapper.map.alg.embed3d.CorrelationType;
import org.chesmapper.map.alg.embed3d.EqualPositionProperty;
import org.chesmapper.map.alg.embed3d.Random3DEmbedder;
import org.chesmapper.map.alg.embed3d.ThreeDEmbedder;
import org.chesmapper.map.data.ClusteringData;
import org.chesmapper.map.data.CompoundDataImpl;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.DefaultFeatureComputer;
import org.chesmapper.map.dataInterface.ClusterData;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertySet;
import org.chesmapper.map.dataInterface.CompoundPropertyUtil;
import org.mg.javalib.util.StringUtil;

public class CheSMapping
{
	DatasetFile dataset;
	FeatureComputer featureComputer;
	DatasetClusterer datasetClusterer;
	ThreeDBuilder threeDGenerator;
	ThreeDEmbedder threeDEmbedder;
	ThreeDAligner threeDAligner;

	Random3DEmbedder randomEmbedder = Random3DEmbedder.INSTANCE;
	UseOrigStructures no3DBuilder = UseOrigStructures.INSTANCE;
	NoClusterer noClusterer = NoClusterer.INSTANCE;
	ThreeDAligner noAligner = NoAligner.INSTANCE;

	public CheSMapping(DatasetFile dataset, CompoundPropertySet features[], DatasetClusterer datasetClusterer,
			ThreeDBuilder threeDGenerator, ThreeDEmbedder threeDEmbedder, ThreeDAligner threeDAligner)
	{
		this.dataset = dataset;
		this.featureComputer = new DefaultFeatureComputer(features);
		this.datasetClusterer = datasetClusterer;
		this.threeDGenerator = threeDGenerator;
		this.threeDEmbedder = threeDEmbedder;
		this.threeDAligner = threeDAligner;
	}

	ClusteringData clusteringData;
	Throwable mappingError;
	Exception threeDBuilderException;
	Exception clusterException;
	Exception embedderException;
	Exception alignException;

	public ClusteringData doMapping()
	{
		clusteringData = null;
		mappingError = null;

		final Thread loadDatasetThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					if (dataset.getCompounds().length == 0)
						throw new IllegalStateException("no compounds");

					TaskProvider.update(10, "Compute 3d structure for compounds");
					build3d(dataset, threeDGenerator);

					ClusteringData clustering = new ClusteringData(dataset);

					if (!TaskProvider.isRunning())
						return;
					TaskProvider.update(20, "Compute features");
					featureComputer.computeFeatures(dataset);
					for (CompoundProperty f : featureComputer.getFeatures())
						clustering.addFeature(f);
					for (CompoundProperty p : featureComputer.getProperties())
						clustering.addProperty(p);
					for (CompoundData c : featureComputer.getCompounds())
						clustering.addCompound(c);
					if (dataset.getCompounds().length != clustering.getCompounds().size())
						throw new IllegalStateException("num compounds does not fit " + dataset.getCompounds().length
								+ " != " + clustering.getCompounds().size());

					List<CompoundProperty> featuresWithInfo = determineFeaturesWithInfo(clustering);

					if (!TaskProvider.isRunning())
						return;
					TaskProvider.update(30, "Compute clusters");
					clusterDataset(dataset, clustering, featuresWithInfo);

					if (!TaskProvider.isRunning())
						return;
					TaskProvider.update(40, "Embed compounds into 3D space");
					embedDataset(dataset, clustering, featuresWithInfo);

					if (clustering.getDatasetClusterer() != noClusterer
							&& clustering.getThreeDEmbedder() != randomEmbedder
							&& !clustering.getDatasetClusterer().getDistanceMeasure()
									.equals(clustering.getThreeDEmbedder().getDistanceMeasure()))
						TaskProvider.warning(
								Settings.text("mapping.incomp-distances"),
								Settings.text("mapping.incomp-distances.desc", clustering.getDatasetClusterer()
										.getDistanceMeasure() + "", clustering.getThreeDEmbedder().getDistanceMeasure()
										+ ""));

					if (!TaskProvider.isRunning())
						return;

					if (Settings.BIG_DATA)
					{
						if (threeDAligner != NoAligner.INSTANCE)
							TaskProvider.warning(Settings.text("mapping.align-ignored"),
									Settings.text("mapping.align-ignored.desc"));
						threeDAligner = BigDataFakeAligner.INSTANCE;
						TaskProvider.update(50, "Create input dataset without structures");
					}
					else
						TaskProvider.update(50, "3D align clusters");
					alignDataset(dataset, clustering);

					//		// make sure the files contain appropriate number of compounds 
					//		int cCount = 0;
					//		for (int[] indices : datasetClusterer.getClusterIndices())
					//			if (SDFUtil.countCompounds(threeDAligner.getAlginedClusterFiles()[cCount++]) != indices.length)
					//				throw new IllegalStateException();

					TaskProvider.update(60, "Mapping complete - load 3D library");

					clustering.setSDF(dataset.getSDFAligned());
					clusteringData = clustering;
				}
				catch (Throwable e)
				{
					mappingError = e;
					Settings.LOGGER.error(e);
					TaskProvider.failed("Mapping process failed", e);
				}
			}
		});
		loadDatasetThread.start();
		try
		{
			loadDatasetThread.join();
		}
		catch (InterruptedException e)
		{
			Settings.LOGGER.error(e);
		}
		return clusteringData;
	}

	/**
	 * featuresWithInfo are those that are actually used for clustering and embedding
	 * featuresWithInfo may be != features
	 * (features does always contain all features selected by the user)
	 * features with unique values are always removed
	 * there is a switch for omitting redundant features (default: true)
	 * (this switch is also used for caching)
	 */
	protected List<CompoundProperty> determineFeaturesWithInfo(ClusteringData clustering)
	{
		List<CompoundProperty> featuresWithInfo = new ArrayList<CompoundProperty>();
		int unique = 0;
		int redundant = 0;
		for (CompoundProperty p : clustering.getFeatures())
			if (p.numDistinctValues() > 1)
				featuresWithInfo.add(p);
			else
			{
				Settings.LOGGER.info("skipping unique prop: " + p);
				unique++;
			}
		if (Settings.SKIP_REDUNDANT_FEATURES)
		{
			CompoundPropertyUtil.determineRedundantFeatures(featuresWithInfo);
			List<CompoundProperty> rem = new ArrayList<CompoundProperty>();
			for (CompoundProperty p : featuresWithInfo)
				if (p.getRedundantProp() != null)
				{
					redundant++;
					Settings.LOGGER.info("skipping redundant prop: " + p);
					rem.add(p);
				}
			if (redundant > 0)
			{
				for (CompoundProperty p : rem)
					featuresWithInfo.remove(p);
				clustering.setSkippingRedundantFeatures(true);
			}
		}
		if (unique > 0)
			TaskProvider.warning(unique + " of " + clustering.getFeatures().size()
					+ " feature/s with equal value for each compound ignored for mapping",
					"Feature/s that have an equal value for each compound, contain no information to distinguish between compounds. "
							+ "The feature/s are ignored by the mapping algorithms (3D-embedding and clustering). "
							+ "In the viewer, the feature/s are in the list of features that are selected for mapping "
							+ "(but labeled as feature with equal value for each compound).");
		if (redundant > 0)
			TaskProvider.warning(redundant + " of " + clustering.getFeatures().size()
					+ " feature/s with redundant values ignored for mapping",
					"Redundant feature/s contain the same information (feature values) as other features in the dataset. "
							+ "The feature/s are ignored by the mapping algorithms (3D-embedding and clustering). "
							+ "In the viewer, the feature/s are in the list of features that are selected for mapping "
							+ "(but labeled as redundant).");
		return featuresWithInfo;
	}

	public Throwable getMappingError()
	{
		return mappingError;
	}

	public Exception getEmbedException()
	{
		return embedderException;
	}

	public Exception getClusterException()
	{
		return clusterException;
	}

	public Exception getAlignException()
	{
		return alignException;
	}

	private void build3d(DatasetFile dataset, ThreeDBuilder builder)
	{
		ThreeDBuilder b = threeDGenerator;
		threeDBuilderException = null;
		try
		{
			b.build3D(dataset);
		}
		catch (Exception e)
		{
			threeDBuilderException = e;
			Settings.LOGGER.error(e);
			if (b == no3DBuilder)
				throw new Error("internal error: no 3d builder should not fail!", e);
			TaskProvider.warning(b.getName() + " failed on building 3d structures", e.getMessage());

			b = no3DBuilder;
			no3DBuilder.build3D(dataset);
		}

		dataset.setSDF3D(b.get3DSDFile());
		if (dataset.getSDF3D() == null)
			throw new IllegalStateException();
		if (!dataset.getSDF().equals(dataset.getSDF3D()))
			dataset.updateCompoundStructureFrom3DSDF();
	}

	private void clusterDataset(DatasetFile dataset, ClusteringData clustering, List<CompoundProperty> featuresWithInfo)
	{
		DatasetClusterer clusterer = datasetClusterer;
		clusterException = null;
		if (dataset.numCompounds() == 1 && !(clusterer instanceof NoClusterer))
		{
			TaskProvider

					.warning(
							"Could not cluster dataset, dataset has only one compound",
							"Clustering divides a dataset into clusters (i.e. into subsets of compounds). There is only one compound in the dataset.");
			clusterer = noClusterer;
		}
		else if (datasetClusterer.requiresFeatures() && featuresWithInfo.size() == 0)
		{
			TaskProvider

					.warning(
							"Could not cluster dataset, as every compound has equal feature values",
							"Clustering divides a dataset into clusters (i.e. into subsets of compounds), according to the feature values of the compounds. "
									+ "The compounds have equal feature values for each feature, and cannot be distinguished by the clusterer.");
			clusterer = noClusterer;
		}
		try
		{
			clustering.setDatasetClusterer(clusterer);
			clusterer.clusterDataset(dataset, clustering.getCompounds(), featuresWithInfo);
			if (!TaskProvider.isRunning())
				return;
		}
		catch (Exception e)
		{
			clusterException = e;
			Settings.LOGGER.error(e);
			if (clusterer == noClusterer)
				throw new Error("internal error: no clusterer should not fail!", e);
			TaskProvider.warning(clusterer.getName() + " failed on clustering dataset", e.getMessage());
			clusterer = noClusterer;
			clustering.setDatasetClusterer(clusterer);
			noClusterer.clusterDataset(dataset, clustering.getCompounds(), featuresWithInfo);
		}

		dataset.setSDFClustered(clusterer.getClusterSDFile());
		if (dataset.getSDFClustered() == null)
			throw new IllegalStateException();

		int sum = 0;
		for (ClusterData c : clusterer.getClusters())
		{
			if (c.getNumCompounds() == 0)
				throw new IllegalStateException("internal error: cluster has size 0");
			sum += c.getNumCompounds();
			clustering.addCluster(c);
		}
		if (sum != clustering.getCompounds().size())
			throw new IllegalStateException("internal error: num clustered compounds does not fit");
	}

	private void embedDataset(DatasetFile dataset, ClusteringData clustering, List<CompoundProperty> featuresWithInfo)
	{
		embedderException = null;
		ThreeDEmbedder embedder = threeDEmbedder;
		if (dataset.numCompounds() == 1 && !(embedder instanceof Random3DEmbedder))
		{
			TaskProvider.warning("Could not embedd dataset, dataset has only one compound",
					"3D Embedding uses feature values to embedd compounds in 3D space, with similar compounds close to each other. "
							+ "There is only one compound in the dataset.");
			embedder = randomEmbedder;
		}
		else if (threeDEmbedder.requiresFeatures() && featuresWithInfo.size() == 0)
		{
			TaskProvider

					.warning(
							"Could not embedd dataset, as every compound has equal feature values, CheS-Mapper uses random positions",
							"3D Embedding uses feature values to embedd compounds in 3D space, with similar compounds close to each other. "
									+ "The compounds have equal feature values for each feature, and cannot be distinguished by the embedder.");
			embedder = randomEmbedder;
		}
		try
		{
			//			CompoundProperty p = null;
			//			for (CompoundProperty pp : clustering.getProperties())
			//				if (pp.toString().equals("dataset"))
			//					p = pp;
			//			if (p == null)
			//				throw new IllegalArgumentException();
			//			for (int i = 0; i < train.length; i++)
			//				train[i] = clustering.getCompounds().get(i).getStringValue(p).equals("train");

			clustering.setThreeDEmbedder(embedder);
			embedder.embedDataset(dataset, clustering.getCompounds(), featuresWithInfo); //, train
			if (!TaskProvider.isRunning())
				return;
		}
		catch (Exception e)
		{
			embedderException = e;
			Settings.LOGGER.error(e);
			if (embedder == randomEmbedder)
				throw new Error("internal error: random embedder should not fail!", e);
			TaskProvider.warning(
					embedder.getName() + " failed on embedding dataset, CheS-Mapper uses random positions",
					e.getMessage());
			embedder = randomEmbedder;
			clustering.setThreeDEmbedder(embedder);
			randomEmbedder.embedDataset(dataset, clustering.getCompounds(), featuresWithInfo); //,null
		}

		//		if (emb.getProcessMessages() != null && emb.getProcessMessages().containsWarning())
		//			TaskProvider.warning("Warning from 3D Embedding " + emb.getName(),
		//					emb.getProcessMessages().getMessage(MessageType.Warning).getString());

		//		clustering.setEmbedAlgorithm(embedder.getName());

		if (dataset.numCompounds() > 2 && !Settings.BIG_DATA)
		{
			double corr = embedder.getCorrelation(CorrelationType.Pearson);
			Settings.LOGGER.info("correlation: " + corr);
			String details = " (Pearson: " + StringUtil.formatDouble(corr, 2) + ")";
			String warnMsg = null;
			// categorization according to evans 1996
			if (corr >= 0.8)
				clustering.setEmbedQuality("very good" + details);
			else if (corr >= 0.6)
				clustering.setEmbedQuality("good" + details);
			else if (corr >= 0.4)
			{
				warnMsg = "The embedding quality is moderate" + details;
				clustering.setEmbedQuality("moderate" + details);
			}
			else if (corr >= 0.2)
			{
				warnMsg = "The embedding quality is weak" + details;
				clustering.setEmbedQuality("weak" + details);
			}
			else
			{
				warnMsg = "The embedding quality is very weak" + details;
				clustering.setEmbedQuality("very weak" + details);
			}
			if (warnMsg != null)
			{
				if (embedder instanceof Random3DEmbedder)
					warnMsg = "Random embedding applied, 3D positions do not reflect feature values";
				TaskProvider.warning(warnMsg, Settings.text("embed.info.quality", Settings.text("embed.r.sammon")));
			}
			for (CorrelationType t : CorrelationType.types())
			{
				if (embedder.getCorrelationProperty(t) != null)
				{
					CorrelationProperty p = embedder.getCorrelationProperty(t);
					for (int i = 0; i < clustering.getNumCompounds(false); i++)
						((CompoundDataImpl) clustering.getCompounds().get(i)).setDoubleValue(p, p.getDoubleValues()[i]);
					clustering.addEmbedQualityProperty(p);
				}
			}
		}
		else
			clustering.setEmbedQuality("n/a");

		if (embedder != randomEmbedder)
		{
			//			EqualPositionProperty eqPos = EqualPositionProperty.create(dataset, embedder.getPositions(),
			//					dataset.getEmbeddingResultsFilePath("eq-pos"));
			EqualPositionProperty eqPos = EqualPositionProperty.create(embedder.getPositions());
			if (eqPos != null)
			{
				for (int i = 0; i < clustering.getNumCompounds(false); i++)
					((CompoundDataImpl) clustering.getCompounds().get(i)).setStringValue(eqPos,
							eqPos.getStringValues()[i]);
				clustering.addEqualPosProperty(eqPos);
			}
		}

		int cCount = 0;
		for (Vector3f v : embedder.getPositions())
			((CompoundDataImpl) clustering.getCompounds().get(cCount++)).setPosition(v);
		for (ClusterData c : clustering.getClusters())
			for (CompoundData co : c.getCompounds())
				if (co.getPosition() == null)
					throw new IllegalStateException("embedd error: position is null!");
	}

	private void alignDataset(DatasetFile dataset, ClusteringData clustering)
	{
		alignException = null;
		ThreeDAligner aligner = threeDAligner;
		try
		{
			clustering.setThreeDAligner(aligner);
			aligner.algin(dataset, clustering.getClusters(), clustering.getFeatures());
		}
		catch (Exception e)
		{
			alignException = e;
			Settings.LOGGER.error(e);
			TaskProvider.warning(aligner.getName() + " failed on aligning dataset", e.getMessage());
			aligner = noAligner;
			clustering.setThreeDAligner(aligner);
			noAligner.algin(dataset, clustering.getClusters(), clustering.getFeatures());
		}
		//		if (aligner.getSubstructureSmartsType() != null)
		//			clustering.addSubstructureSmartsTypes(aligner.getSubstructureSmartsType());
		//		for (int i = 0; i < clustering.getNumClusters(); i++)
		//			((ClusterDataImpl) clustering.getCluster(i)).setAlignedFilename(align.getAlginedClusterFile(i));

		dataset.setSDFAligned(aligner.getAlignedSDFile());
		if (dataset.getSDFAligned() == null)
			throw new IllegalStateException();
	}

	public DatasetFile getDatasetFile()
	{
		return dataset;
	}

	public int getNumFeatureSets()
	{
		return featureComputer.getNumFeatureSets();
	}

}
