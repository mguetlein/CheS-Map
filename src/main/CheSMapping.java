package main;

import util.SwingUtil;
import alg.DatasetProvider;
import alg.FeatureComputer;
import alg.align3d.ThreeDAligner;
import alg.build3d.OpenBabel3DBuilder;
import alg.build3d.ThreeDBuilder;
import alg.cluster.DatasetClusterer;
import alg.embed3d.Random3DEmbedder;
import alg.embed3d.ThreeDEmbedder;
import data.ClusterDataImpl;
import data.ClusteringData;
import data.DatasetFile;
import data.DefaultFeatureComputer;
import data.EmbedClusters;
import data.FeatureService;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;

public class CheSMapping
{
	DatasetProvider datasetProvider;
	FeatureComputer featureComputer;
	DatasetClusterer datasetClusterer;
	ThreeDBuilder threeDGenerator;
	ThreeDEmbedder threeDEmbedder;
	ThreeDAligner threeDAligner;

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

	public ClusteringData doMapping()
	{
		clusteringData = null;
		final Thread loadDatasetThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					TaskProvider.registerThread("Ches-Mapper-Task");

					DatasetFile dataset = datasetProvider.getDatasetFile();

					if (dataset.getSDFPath(false) == null)
					{
						TaskProvider.task().update(0, "convert dataset to sdf file");
						FeatureService.writeSDFFile(dataset);
					}

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

					if (TaskProvider.task().isCancelled())
						return;
					TaskProvider.task().update(30, "Compute clusters");
					datasetClusterer.clusterDataset(dataset, clustering.getCompounds(), clustering.getFeatures());
					for (ClusterData c : datasetClusterer.getClusters())
						clustering.addCluster(c);
					clustering.setClusterAlgorithm(datasetClusterer.getName());

					if (TaskProvider.task().isCancelled())
						return;
					TaskProvider.task().update(40,
							"Embedding clusters into 3D space (num clusters: " + clustering.getClusters().size() + ")");
					EmbedClusters embedder = new EmbedClusters();
					embedder.embed(threeDEmbedder, dataset, clustering);
					clustering.setEmbedAlgorithm(threeDEmbedder.getName());

					if (TaskProvider.task().isCancelled())
						return;
					TaskProvider.task().update(50, "3D align compounds");
					threeDAligner.algin(dataset, clustering.getClusters(), clustering.getFeatures());
					if (threeDAligner.getSubstructureSmartsType() != null)
						clustering.addSubstructureSmartsTypes(threeDAligner.getSubstructureSmartsType());
					int cCount = 0;
					for (String alignedFile : threeDAligner.getAlginedClusterFiles())
						((ClusterDataImpl) clustering.getClusters().get(cCount++)).setFilename(alignedFile);

					//		// make sure the files contain appropriate number of compounds 
					//		int cCount = 0;
					//		for (int[] indices : datasetClusterer.getClusterIndices())
					//			if (SDFUtil.countCompounds(threeDAligner.getAlginedClusterFiles()[cCount++]) != indices.length)
					//				throw new IllegalStateException();

					TaskProvider.task().update(60, "Mapping complete - Loading 3D library");

					for (ClusterData c : clustering.getClusters())
					{
						for (CompoundData co : c.getCompounds())
							if (co.getPosition() == null)
								throw new Error("internal error: cluster position is null!");
					}

					clusteringData = clustering;
				}
				catch (Throwable e)
				{
					e.printStackTrace();
					TaskProvider.task().error(e.getMessage(), e);
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
}
