package main;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import util.ListUtil;
import util.SwingUtil;
import alg.DatasetProvider;
import alg.FeatureComputer;
import alg.align3d.NoAligner;
import alg.align3d.ThreeDAligner;
import alg.build3d.OpenBabel3DBuilder;
import alg.build3d.ThreeDBuilder;
import alg.cluster.DatasetClusterer;
import alg.cluster.NoClusterer;
import alg.embed3d.Random3DEmbedder;
import alg.embed3d.ThreeDEmbedder;
import data.ClusterDataImpl;
import data.ClusteringData;
import data.CompoundDataImpl;
import data.DatasetFile;
import data.DefaultFeatureComputer;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculePropertyUtil;

public class CheSMapping
{
	DatasetProvider datasetProvider;
	FeatureComputer featureComputer;
	DatasetClusterer datasetClusterer;
	ThreeDBuilder threeDGenerator;
	ThreeDEmbedder threeDEmbedder;
	ThreeDAligner threeDAligner;

	Random3DEmbedder randomEmbedder = new Random3DEmbedder();
	NoClusterer noClusterer = new NoClusterer();
	ThreeDAligner noAligner = new NoAligner();

	public static CheSMapping testWorkflow()
	{
		DatasetProvider datasetProvider = new DatasetProvider()
		{
			@Override
			public DatasetFile getDatasetFile()
			{
				return null;
			}
		};
		FeatureComputer featureComputer = new DefaultFeatureComputer();
		//DatasetClusterer datasetClusterer = new KMeansClusterer();
		DatasetClusterer datasetClusterer = null; //mew StructuralClusterer();
		ThreeDBuilder threeDGenerator = new OpenBabel3DBuilder();
		ThreeDEmbedder threeDEmbedder = new Random3DEmbedder();
		ThreeDAligner threeDAligner = null;

		return new CheSMapping(datasetProvider, featureComputer, datasetClusterer, threeDGenerator, threeDEmbedder,
				threeDAligner);
	}

	public CheSMapping(DatasetProvider datasetProvider, FeatureComputer featureComputer,
			DatasetClusterer datasetClusterer, ThreeDBuilder threeDGenerator, ThreeDEmbedder threeDEmbedder,
			ThreeDAligner threeDAligner)
	{
		this.datasetProvider = datasetProvider;
		this.featureComputer = featureComputer;
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
					TaskProvider.registerThread("Ches-Mapper-Task");

					DatasetFile dataset = datasetProvider.getDatasetFile();

					TaskProvider.task().update(10, "Compute 3d compound structures");
					threeDGenerator.build3D(dataset);
					dataset.setSDFPath(threeDGenerator.get3DSDFFile(), true);

					ClusteringData clustering = new ClusteringData(dataset.getName(), dataset.getSDFPath(true));

					if (TaskProvider.task().isCancelled())
						return;
					TaskProvider.task().update(20, "Compute features");
					featureComputer.computeFeatures(dataset);
					for (MoleculeProperty f : featureComputer.getFeatures())
						clustering.addFeature(f);
					for (MoleculeProperty p : featureComputer.getProperties())
						clustering.addProperty(p);
					for (CompoundData c : featureComputer.getCompounds())
						clustering.addCompound(c);

					List<MoleculeProperty> featuresWithInfo = new ArrayList<MoleculeProperty>();
					for (MoleculeProperty p : clustering.getFeatures())
						if (!MoleculePropertyUtil.hasUniqueValue(p, dataset))
							featuresWithInfo.add(p);

					if (TaskProvider.task().isCancelled())
						return;
					TaskProvider.task().update(30, "Compute clusters");
					clusterDataset(dataset, clustering, featuresWithInfo);

					if (TaskProvider.task().isCancelled())
						return;
					TaskProvider.task().update(40,
							"Embedding clusters into 3D space (num clusters: " + clustering.getClusters().size() + ")");
					embedDataset(dataset, clustering, featuresWithInfo);

					if (TaskProvider.task().isCancelled())
						return;
					TaskProvider.task().update(50, "3D align compounds");
					alignDataset(dataset, clustering);

					//		// make sure the files contain appropriate number of compounds 
					//		int cCount = 0;
					//		for (int[] indices : datasetClusterer.getClusterIndices())
					//			if (SDFUtil.countCompounds(threeDAligner.getAlginedClusterFiles()[cCount++]) != indices.length)
					//				throw new IllegalStateException();

					TaskProvider.task().update(60, "Mapping complete - Loading 3D library");

					clusteringData = clustering;
				}
				catch (Throwable e)
				{
					mappingError = e;
					e.printStackTrace();
					TaskProvider.task().error(e.getMessage(), e);
					if (TaskProvider.task().getDialog() != null)
						SwingUtil.waitWhileVisible(TaskProvider.task().getDialog());
					TaskProvider.clear();
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
			e.printStackTrace();
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

	private void clusterDataset(DatasetFile dataset, ClusteringData clustering, List<MoleculeProperty> featuresWithInfo)
	{
		DatasetClusterer clusterer = datasetClusterer;
		clusterException = null;
		if (dataset.numCompounds() == 1)
		{
			TaskProvider
					.task()
					.warning(
							"Could not cluster dataset, dataset has only one compound",
							"Clustering divides a dataset into clusters (i.e. into subsets of compounds). There is only one compound in the dataset.");
			clusterer = noClusterer;
		}
		else if (datasetClusterer.requiresFeatures() && featuresWithInfo.size() == 0)
		{
			TaskProvider
					.task()
					.warning(
							"Could not cluster dataset, as every compound has equal feature values",
							"Clustering divides a dataset into clusters (i.e. into subsets of compounds), according to the feature values of the compounds. "
									+ "The compounds have equal feature values for each feature, and cannot be distinguished by the clusterer.");
			clusterer = noClusterer;
		}
		try
		{
			clusterer.clusterDataset(dataset, clustering.getCompounds(), featuresWithInfo);
		}
		catch (Exception e)
		{
			clusterException = e;
			e.printStackTrace();
			if (clusterer == noClusterer)
				throw new Error("internal error: no clusterer should not fail!", e);
			TaskProvider.task().warning(clusterer.getName() + " failed on clustering dataset", e.getMessage());
			clusterer = noClusterer;
			noClusterer.clusterDataset(dataset, clustering.getCompounds(), featuresWithInfo);
		}
		for (ClusterData c : clusterer.getClusters())
			clustering.addCluster(c);
		clustering.setClusterAlgorithm(clusterer.getName());
	}

	private void embedDataset(DatasetFile dataset, ClusteringData clustering, List<MoleculeProperty> featuresWithInfo)
	{
		embedderException = null;
		ThreeDEmbedder emb = threeDEmbedder;
		if (dataset.numCompounds() == 1)
		{
			TaskProvider.task().warning(
					"Could not embedd dataset, dataset has only one compound",
					"3D Embedding uses feature values to embedd compounds in 3D space, with similar compounds close to each other. "
							+ "There is only one compound in the dataset.");
			emb = randomEmbedder;
		}
		else if (threeDEmbedder.requiresFeatures() && featuresWithInfo.size() == 0)
		{
			TaskProvider
					.task()
					.warning(
							"Could not embedd dataset, as every compound has equal feature values, using random positions",
							"3D Embedding uses feature values to embedd compounds in 3D space, with similar compounds close to each other. "
									+ "The compounds have equal feature values for each feature, and cannot be distinguished by the embedder.");
			emb = randomEmbedder;
		}
		try
		{
			emb.embed(dataset, ListUtil.cast(MolecularPropertyOwner.class, clustering.getCompounds()), featuresWithInfo);
		}
		catch (Exception e)
		{
			embedderException = e;
			e.printStackTrace();
			if (emb == randomEmbedder)
				throw new Error("internal error: random embedder should not fail!", e);
			TaskProvider.task().warning(emb.getName() + " failed on embedding dataset, using random positions",
					e.getMessage());
			emb = randomEmbedder;
			randomEmbedder.embed(dataset, ListUtil.cast(MolecularPropertyOwner.class, clustering.getCompounds()), null);
		}

		clustering.setEmbedAlgorithm(emb.getName());
		int cCount = 0;
		for (Vector3f v : emb.getPositions())
			((CompoundDataImpl) clustering.getCompounds().get(cCount++)).setPosition(v);
		for (ClusterData c : clustering.getClusters())
			for (CompoundData co : c.getCompounds())
				if (co.getPosition() == null)
					throw new IllegalStateException("embedd error: position is null!");
	}

	private void alignDataset(DatasetFile dataset, ClusteringData clustering)
	{
		alignException = null;
		ThreeDAligner align = threeDAligner;
		try
		{
			align.algin(dataset, clustering.getClusters(), clustering.getFeatures());
		}
		catch (Exception e)
		{
			alignException = e;
			e.printStackTrace();
			TaskProvider.task().warning(align.getName() + " failed on aligning dataset", e.getMessage());
			align = noAligner;
			noAligner.algin(dataset, clustering.getClusters(), clustering.getFeatures());
		}
		if (align.getSubstructureSmartsType() != null)
			clustering.addSubstructureSmartsTypes(align.getSubstructureSmartsType());
		int cCount = 0;
		for (String alignedFile : align.getAlginedClusterFiles())
			((ClusterDataImpl) clustering.getClusters().get(cCount++)).setFilename(alignedFile);
	}

}
