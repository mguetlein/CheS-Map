package main;

import gui.Progressable;
import gui.SubProgress;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import util.ListUtil;
import alg.DatasetProvider;
import alg.FeatureComputer;
import alg.align3d.ThreeDAligner;
import alg.build3d.OpenBabel3DBuilder;
import alg.build3d.ThreeDBuilder;
import alg.cluster.DatasetClusterer;
import alg.cluster.StructuralClusterer;
import alg.embed3d.Random3DEmbedder;
import alg.embed3d.ThreeDEmbedder;
import data.CDKFeatureComputer;
import data.CDKService;
import data.ClusterDataImpl;
import data.ClusteringData;
import data.DatasetFile;
import data.EmbedClusters;
import data.MCSComputer;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;
import dataInterface.SubstructureSmartsType;

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
		FeatureComputer featureComputer = new CDKFeatureComputer();
		//DatasetClusterer datasetClusterer = new KMeansClusterer();
		DatasetClusterer datasetClusterer = new StructuralClusterer();
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

	public ClusteringData doMapping(final Progressable progress)
	{
		clusteringData = null;
		final Thread loadDatasetThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					double step = 100 / 8.0;

					DatasetFile dataset = datasetProvider.getDatasetFile();

					if (dataset.getSDFPath(false) == null)
					{
						progress.update(0, "convert dataset to sdf file");
						CDKService.writeSDFFile(dataset);
					}

					if (progress != null)
						progress.update(step, "compute 3d compound structures");
					System.out.println("compute 3d compound structures");
					threeDGenerator.build3D(dataset, SubProgress.create(progress, step, step * 2));
					dataset.setSDFPath(threeDGenerator.get3DSDFFile(), true);

					ClusteringData clustering = new ClusteringData(dataset.getName(), dataset.getSDFPath(true));

					if (Settings.isAborted(Thread.currentThread()))
						return;
					if (progress != null)
						progress.update(step * 2, "compute features");
					System.out.println("compute features");
					featureComputer.computeFeatures(dataset);
					for (MoleculeProperty f : featureComputer.getFeatures())
						clustering.addFeature(f);
					for (MoleculeProperty p : featureComputer.getProperties())
						clustering.addProperty(p);
					for (CompoundData c : featureComputer.getCompounds())
						clustering.addCompound(c);

					if (Settings.isAborted(Thread.currentThread()))
						return;
					if (progress != null)
						progress.update(step * 3, "compute clusters");
					System.out.println("compute clusters");
					datasetClusterer.clusterDataset(dataset, clustering.getCompounds(), clustering.getFeatures(),
							SubProgress.create(progress, step * 3, step * 4));
					for (ClusterData c : datasetClusterer.getClusters())
						clustering.addCluster(c);

					if (threeDEmbedder.requiresDistances())
					{
						if (progress != null)
							progress.update(step * 4, "compute distances");
						System.out.println("compute distances");
						clustering.getClusterDistances();
						for (ClusterDataImpl c : ListUtil.cast(ClusterDataImpl.class, clustering.getClusters()))
							c.getCompoundDistances(clustering.getFeatures());
					}

					if (Settings.isAborted(Thread.currentThread()))
						return;
					if (progress != null)
						progress.update(step * 5, "3D embedding clusters");
					System.out.println("3D embedding clusters (" + clustering.getClusters().size() + ")");
					EmbedClusters embedder = new EmbedClusters();
					embedder.embed(threeDEmbedder, dataset, clustering,
							SubProgress.create(progress, step * 5, step * 6));

					if (Settings.isAborted(Thread.currentThread()))
						return;
					if (progress != null)
						progress.update(step * 6, "Compute MCS of clusters");
					System.out.println("Compute MCS of clusters");
					MCSComputer.computeMCS(dataset, clustering.getClusters(),
							SubProgress.create(progress, step * 6, step * 7));
					clustering.addSubstructureSmartsTypes(SubstructureSmartsType.MCS);

					if (Settings.isAborted(Thread.currentThread()))
						return;
					if (progress != null)
						progress.update(step * 7, "3D align compounds");
					System.out.println("3D align compounds");
					threeDAligner.algin(dataset, clustering.getClusters());
					int cCount = 0;
					for (String alignedFile : threeDAligner.getAlginedClusterFiles())
						((ClusterDataImpl) clustering.getClusters().get(cCount++)).setFilename(alignedFile);

					//		// make sure the files contain appropriate number of compounds 
					//		int cCount = 0;
					//		for (int[] indices : datasetClusterer.getClusterIndices())
					//			if (SDFUtil.countCompounds(threeDAligner.getAlginedClusterFiles()[cCount++]) != indices.length)
					//				throw new IllegalStateException();

					if (progress != null)
						progress.update(100, "Clustering complete");

					clusteringData = clustering;
				}
				catch (Throwable e)
				{
					e.printStackTrace();
					progress.error(e.getMessage(), e);
					progress.waitForClose();
				}
			}
		});
		progress.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(Progressable.PROPERTY_ABORT))
				{
					Settings.abortThread(loadDatasetThread);
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
