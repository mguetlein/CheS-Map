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
	public void embed(ThreeDEmbedder clusterEmbedder, DatasetFile dataset, ClusteringData clustering,
			Progressable progress)
	{
		System.out.println("embed " + clustering.getClusters().size() + " clusters");
		clusterEmbedder.embed(dataset, ListUtil.cast(MolecularPropertyOwner.class, clustering.getClusters()),
				clustering.getFeatures(), clustering.getClusterDistances().cast(MolecularPropertyOwner.class));
		int cCount = 0;
		for (Vector3f v : clusterEmbedder.getPositions())
			((ClusterDataImpl) clustering.getClusters().get(cCount++)).setPosition(v);
		if (progress != null)
			progress.update(100 * 1 / (double) (clustering.getClusters().size() + 1), "Embedded clusters into 3D-space");

		System.out.println("embed clusters compounds");
		cCount = 0;
		for (final ClusterData cluster : clustering.getClusters())
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

			clusterEmbedder.embed(dataset, ListUtil.cast(MolecularPropertyOwner.class, c.getCompounds()),
					clustering.getFeatures(),
					c.getCompoundDistances(clustering.getFeatures()).cast(MolecularPropertyOwner.class));
			int mCount = 0;
			for (Vector3f v : clusterEmbedder.getPositions())
				((CompoundDataImpl) c.getCompounds().get(mCount++)).setPosition(v);

			if (progress != null)
				progress.update(100 * (2 + cCount) / (double) (clustering.getClusters().size() + 1),
						"Embedded compounds in " + (cCount + 1) + " of " + clustering.getClusters().size()
								+ " clusters");
			cCount++;
		}
	}
}
