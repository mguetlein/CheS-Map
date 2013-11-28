package main;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import util.StringUtil;
import alg.FeatureComputer;
import alg.align3d.NoAligner;
import alg.align3d.ThreeDAligner;
import alg.build3d.ThreeDBuilder;
import alg.cluster.DatasetClusterer;
import alg.cluster.NoClusterer;
import alg.embed3d.Random3DEmbedder;
import alg.embed3d.ThreeDEmbedder;
import data.ClusteringData;
import data.CompoundDataImpl;
import data.DatasetFile;
import data.DefaultFeatureComputer;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundPropertySet;
import dataInterface.CompoundPropertyUtil;

public class CheSMapping
{
	DatasetFile dataset;
	FeatureComputer featureComputer;
	DatasetClusterer datasetClusterer;
	ThreeDBuilder threeDGenerator;
	ThreeDEmbedder threeDEmbedder;
	ThreeDAligner threeDAligner;

	Random3DEmbedder randomEmbedder = Random3DEmbedder.INSTANCE;
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
					TaskProvider.update(10, "Compute 3d structure for compounds");
					threeDGenerator.build3D(dataset);
					dataset.setSDF3D(threeDGenerator.get3DSDFile());
					if (dataset.getSDF3D() == null)
						throw new IllegalStateException();
					if (!dataset.getSDF().equals(dataset.getSDF3D()))
						dataset.updateCompoundStructureFrom3DSDF();

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

					List<CompoundProperty> featuresWithInfo = new ArrayList<CompoundProperty>();
					for (CompoundProperty p : clustering.getFeatures())
						if (!CompoundPropertyUtil.hasUniqueValue(p, dataset))
							featuresWithInfo.add(p);

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
								Settings.text("mapping.incomp.distances"),
								Settings.text("mapping.incomp.distances.desc", clustering.getDatasetClusterer()
										.getDistanceMeasure() + "", clustering.getThreeDEmbedder().getDistanceMeasure()
										+ ""));

					if (!TaskProvider.isRunning())
						return;
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

		for (ClusterData c : clusterer.getClusters())
			clustering.addCluster(c);
	}

	@SuppressWarnings("unchecked")
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
			clustering.setThreeDEmbedder(embedder);
			embedder.embedDataset(dataset, clustering.getCompounds(), featuresWithInfo);
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
			randomEmbedder.embedDataset(dataset, clustering.getCompounds(), featuresWithInfo);
		}

		//		if (emb.getProcessMessages() != null && emb.getProcessMessages().containsWarning())
		//			TaskProvider.warning("Warning from 3D Embedding " + emb.getName(),
		//					emb.getProcessMessages().getMessage(MessageType.Warning).getString());

		//		clustering.setEmbedAlgorithm(embedder.getName());

		if (dataset.numCompounds() > 2)
		{
			double rSquare = embedder.getRSquare();
			double ccc = embedder.getCCC();
			Settings.LOGGER.info("r-square: " + rSquare + ", ccc: " + ccc);
			String formRSquare = StringUtil.formatDouble(rSquare, 2);
			String formCCC = StringUtil.formatDouble(embedder.getCCC(), 2);
			String details = " (CCC: " + formCCC + ", r^2: " + formRSquare + ")";
			String warnMsg = null;
			if (ccc >= 0.9)
				clustering.setEmbedQuality("excellent" + details);
			else if (ccc >= 0.7)
				clustering.setEmbedQuality("good" + details);
			else if (ccc >= 0.5)
			{
				warnMsg = "The embedding quality is moderate" + details;
				clustering.setEmbedQuality("moderate" + details);
			}
			else
			{ // < 0.5
				warnMsg = "The embedding quality is poor" + details;
				clustering.setEmbedQuality("poor" + details);
			}
			if (warnMsg != null)
			{
				if (embedder instanceof Random3DEmbedder)
					warnMsg = "Random embedding applied, 3D positions do not reflect feature values" + details;
				TaskProvider.warning(warnMsg, Settings.text("embed.info.quality", Settings.text("embed.r.sammon")));
			}
			if (embedder.getCCCProperty() != null)
			{
				for (int i = 0; i < clustering.getNumCompounds(false); i++)
					((CompoundDataImpl) clustering.getCompounds().get(i)).setDoubleValue(embedder.getCCCProperty(),
							embedder.getCCCProperty().getDoubleValues(dataset)[i]);
				embedder.getCCCProperty().setMappedDataset(dataset);
				clustering.setEmbeddingQualityProperty(embedder.getCCCProperty());
			}

			//			List<CompoundProperty> distanceTo = new ArrayList<CompoundProperty>();
			//			int i = 0;
			//			for (CompoundData c : clustering.getCompounds())
			//			{
			//				DistanceToProperty p = new DistanceToProperty(dataset, c.getIndex() + "", i, emb.getPositions());
			//				p.setMappedDataset(dataset);
			//				for (int j = 0; j < clustering.getNumCompounds(true); j++)
			//					((CompoundDataImpl) clustering.getCompounds().get(j)).setDoubleValue(p,
			//							p.getDoubleValues(dataset)[j]);
			//				distanceTo.add(p);
			//				i++;
			//			}
			//			clustering.setDistanceToProperties(distanceTo);
		}
		else
			clustering.setEmbedQuality("n/a");

		//		for (CompoundProperty p : ListUtil.concat(clustering.getProperties(), clustering.getFeatures()))
		//			clustering.setEmbeddingQuality(
		//					p,
		//					emb.getEmbedQuality(p, dataset,
		//							ListUtil.cast(MolecularPropertyOwner.class, clustering.getCompounds())));
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

}
