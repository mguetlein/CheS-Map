package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import util.ArrayUtil;
import util.StringUtil;
import alg.FeatureComputer;
import alg.align3d.NoAligner;
import alg.align3d.ThreeDAligner;
import alg.build3d.ThreeDBuilder;
import alg.build3d.UseOrigStructures;
import alg.cluster.DatasetClusterer;
import alg.cluster.NoClusterer;
import alg.embed3d.Random3DEmbedder;
import alg.embed3d.ThreeDEmbedder;
import appdomain.AppDomainComputer;
import data.ClusteringData;
import data.CompoundDataImpl;
import data.DatasetFile;
import data.DefaultFeatureComputer;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;
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
					Settings.MAPPED_DATASET = dataset;

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

					//computedAppDomain(dataset, clustering, featuresWithInfo);

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
			if (c.getSize() == 0)
				throw new IllegalStateException("internal error: cluster has size 0");
			sum += c.getSize();
			clustering.addCluster(c);
		}
		if (sum != clustering.getCompounds().size())
			throw new IllegalStateException("internal error: num clustered compounds does not fit");
	}

	private void computedAppDomain(DatasetFile dataset, ClusteringData clustering,
			List<CompoundProperty> featuresWithInfo)
	{
		for (CompoundProperty p : featuresWithInfo)
			if (p.getType() != Type.NUMERIC || p.numMissingValues(dataset) > 0)
				return;

		//AppDomainComputer appDomain[] = new AppDomainComputer[] { AppDomainHelper.select() };
		AppDomainComputer appDomain[] = AppDomainComputer.APP_DOMAIN_COMPUTERS;
		if (appDomain != null && appDomain.length > 0)
		{
			List<CompoundProperty> props = new ArrayList<CompoundProperty>();
			for (AppDomainComputer ad : appDomain)
			{
				ad.computeAppDomain(dataset, clustering.getCompounds(), featuresWithInfo, clustering
						.getThreeDEmbedder().getFeatureDistanceMatrix().getValues());
				props.add(ad.getInsideAppDomainProperty());
				props.add(ad.getPropabilityAppDomainProperty());
				for (int i = 0; i < clustering.getNumCompounds(false); i++)
				{
					((CompoundDataImpl) clustering.getCompounds().get(i)).setDoubleValue(
							ad.getPropabilityAppDomainProperty(),
							ad.getPropabilityAppDomainProperty().getDoubleValues(dataset)[i]);
					((CompoundDataImpl) clustering.getCompounds().get(i)).setStringValue(
							ad.getInsideAppDomainProperty(),
							ad.getInsideAppDomainProperty().getStringValues(dataset)[i]);
				}
			}

			for (CompoundProperty p : props)
				clustering.addAdditionalProperty(p, false);
		}
	}

	static class MyVector3f extends Vector3f // to fix missing overwrite of equals(Object) in old vecmatch lib included in the cdk 1.14.18
	{
		public MyVector3f(Tuple3f v)
		{
			super(v);
		}

		@Override
		public boolean equals(Object o)
		{
			return o instanceof MyVector3f && super.equals(this);
		}
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

		if (dataset.numCompounds() > 2)
		{
			//			double rSquare = embedder.getRSquare();
			double ccc = embedder.getCCC();
			//			Settings.LOGGER.info("r-square: " + rSquare + ", ccc: " + ccc);
			Settings.LOGGER.info("ccc: " + ccc);
			//			String formRSquare = StringUtil.formatDouble(rSquare, 2);
			String formCCC = StringUtil.formatDouble(embedder.getCCC(), 2);
			String details = " (CCC: " + formCCC + ")";
			//			String details = " (CCC: " + formCCC + ", r^2: " + formRSquare + ")";
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
					warnMsg = "Random embedding applied, 3D positions do not reflect feature values";
				TaskProvider.warning(warnMsg, Settings.text("embed.info.quality", Settings.text("embed.r.sammon")));
			}
			if (embedder.getCCCProperty() != null)
			{
				for (int i = 0; i < clustering.getNumCompounds(false); i++)
					((CompoundDataImpl) clustering.getCompounds().get(i)).setDoubleValue(embedder.getCCCProperty(),
							embedder.getCCCProperty().getDoubleValues(dataset)[i]);
				clustering.addAdditionalProperty(embedder.getCCCProperty(), true);
			}
		}
		else
			clustering.setEmbedQuality("n/a");

		int cCount = 0;
		HashMap<Vector3f, List<Integer>> posMap = new HashMap<Vector3f, List<Integer>>();
		for (Vector3f v : embedder.getPositions())
		{
			((CompoundDataImpl) clustering.getCompounds().get(cCount)).setPosition(v);
			MyVector3f w = new MyVector3f(v);
			if (posMap.containsKey(w))
				posMap.get(w).add(cCount);
			else
				posMap.put(w, ArrayUtil.toList(new int[] { cCount }));
			cCount++;
		}

		int numDistinctPos = posMap.size();
		if (numDistinctPos < cCount)
		{
			int numMultiCompounds = 0;
			int numCommonPos = 0;
			for (List<Integer> l : posMap.values())
				if (l.size() > 1)
				{
					numCommonPos++;
					numMultiCompounds += l.size();
				}
			TaskProvider
					.warning(
							"The " + cCount + " compounds have been mapped to only " + numDistinctPos
									+ " distinct 3D positions",
							"The 3D position of "
									+ numMultiCompounds
									+ " compounds is not unique; "
									+ numCommonPos
									+ " positions are occupied by multiple compounds.\n"
									+ "Embedding algorithms do assign 3D positions based on the feature values. "
									+ "If compounds have equal feature values, they will most likely be assigned equal positions in 3D space. "
									+ "To avoid this, add more features, that help to distinguish between compounds.\n"
									+ "This warning is expected if compounds occur multiple times in the dataset.");
		}

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
