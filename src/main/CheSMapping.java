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
import data.ClusterDataImpl;
import data.ClusteringData;
import data.EmbedClusters;
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
			public String getDatasetName()
			{
				return null;
			}

			@Override
			public String getDatasetFile()
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

	ClusteringData dataset;

	public ClusteringData doMapping(final Progressable progress)
	{
		dataset = null;
		final Thread loadDatasetThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					double step = 100 / 6.0;

					ClusteringData cDataset = new ClusteringData(datasetProvider.getDatasetName());
					cDataset.setFilename(datasetProvider.getDatasetFile());

					if (progress != null)
						progress.update(0, "compute 3d compound structures");
					System.out.println("compute 3d compound structures");
					threeDGenerator.build3D(cDataset.getFilename(), SubProgress.create(progress, 0, 20));
					cDataset.setFilename(threeDGenerator.get3DFile());

					if (Settings.isAborted(Thread.currentThread()))
						return;
					if (progress != null)
						progress.update(step, "compute features");
					System.out.println("compute features");
					featureComputer.computeFeatures(threeDGenerator.get3DFile());
					for (MoleculeProperty f : featureComputer.getFeatures())
						cDataset.addFeature(f);
					for (MoleculeProperty p : featureComputer.getProperties())
						cDataset.addProperty(p);
					for (CompoundData c : featureComputer.getCompounds())
						cDataset.addCompound(c);

					if (Settings.isAborted(Thread.currentThread()))
						return;
					if (progress != null)
						progress.update(step * 2, "compute clusters");
					System.out.println("compute clusters");
					datasetClusterer.clusterDataset(cDataset.getName(), cDataset.getFilename(),
							cDataset.getCompounds(), cDataset.getFeatures());
					for (ClusterData c : datasetClusterer.getClusters())
						cDataset.addCluster(c);

					if (threeDEmbedder.requiresDistances())
					{
						if (progress != null)
							progress.update(step * 3, "compute distances");
						System.out.println("compute distances");
						cDataset.getClusterDistances();
						for (ClusterDataImpl c : ListUtil.cast(ClusterDataImpl.class, cDataset.getClusters()))
							c.getCompoundDistances(cDataset.getFeatures());
					}

					if (Settings.isAborted(Thread.currentThread()))
						return;
					if (progress != null)
						progress.update(step * 4, "3D embedding clusters");
					System.out.println("3D embedding clusters (" + cDataset.getClusters().size() + ")");
					EmbedClusters embedder = new EmbedClusters();
					embedder.embed(threeDEmbedder, cDataset, SubProgress.create(progress, step * 4, step * 5));

					if (Settings.isAborted(Thread.currentThread()))
						return;
					if (progress != null)
						progress.update(step * 5, "3D align compounds");
					System.out.println("3D align compounds");
					threeDAligner.algin(cDataset.getClusters());
					int cCount = 0;
					for (String alignedFile : threeDAligner.getAlginedClusterFiles())
						((ClusterDataImpl) cDataset.getClusters().get(cCount++)).setFilename(alignedFile);
					cDataset.setClusterFilesAligned(threeDAligner.isRealAligner());

					//		// make sure the files contain appropriate number of compounds 
					//		int cCount = 0;
					//		for (int[] indices : datasetClusterer.getClusterIndices())
					//			if (SDFUtil.countCompounds(threeDAligner.getAlginedClusterFiles()[cCount++]) != indices.length)
					//				throw new IllegalStateException();

					if (progress != null)
						progress.update(100, "Clustering complete");

					dataset = cDataset;
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
		return dataset;
	}
}
