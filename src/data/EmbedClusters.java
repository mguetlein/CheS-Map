package data;

import gui.Progressable;

import javax.vecmath.Vector3f;

import main.Settings;
import util.ListUtil;
import alg.embed3d.ThreeDEmbedder;
import dataInterface.ClusterData;
import dataInterface.MolecularPropertyOwner;

public class EmbedClusters
{
	public void embed(ThreeDEmbedder clusterEmbedder, final ClusteringData dataset, Progressable progress)
	{

		System.out.println("embed " + dataset.getClusters().size() + " clusters");
		clusterEmbedder.embed(dataset.getFilename(),
				ListUtil.cast(MolecularPropertyOwner.class, dataset.getClusters()), dataset.getFeatures(), dataset
						.getClusterDistances().cast(MolecularPropertyOwner.class));
		int cCount = 0;
		for (Vector3f v : clusterEmbedder.getPositions())
			((ClusterDataImpl) dataset.getClusters().get(cCount++)).setPosition(v);
		if (progress != null)
			progress.update(100 * 1 / (double) (dataset.getClusters().size() + 1), "Embedded clusters into 3D-space");

		System.out.println("embed clusters compounds");
		cCount = 0;
		for (final ClusterData cluster : dataset.getClusters())
		{
			ClusterDataImpl c = (ClusterDataImpl) cluster;
			System.out.println("embed " + c.getCompounds().size() + " compounds");

			if (Settings.DBG)
			{
				//				System.out.println("compound features:");
				//				for (Compound cc : c.getCompounds())
				//					System.out.println(" " + cc.getValuesString(true));
			}

			//			if (Settings.DBG)
			//				System.out.println("compound distances:\n" + ArrayUtil.toString(clusterCompoundDistances, true));
			//			if (clusterCompoundFeatures.size() > 1)
			//			{
			//				ArrayUtil.normalize(clusterCompoundDistances);
			//				if (Settings.DBG)
			//					System.out.println("normalized compound distances:\n"
			//							+ ArrayUtil.toString(clusterCompoundDistances, true));
			//			}

			clusterEmbedder.embed(dataset.getFilename(), ListUtil.cast(MolecularPropertyOwner.class, c.getCompounds()),
					dataset.getFeatures(),
					c.getCompoundDistances(dataset.getFeatures()).cast(MolecularPropertyOwner.class));
			int mCount = 0;
			for (Vector3f v : clusterEmbedder.getPositions())
				((CompoundDataImpl) c.getCompounds().get(mCount++)).setPosition(v);

			if (progress != null)
				progress.update(100 * (2 + cCount) / (double) (dataset.getClusters().size() + 1),
						"Embedded compounds in " + (cCount + 1) + " of " + dataset.getClusters().size() + " clusters");
			cCount++;
		}
	}
}
