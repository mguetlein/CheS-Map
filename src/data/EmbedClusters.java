package data;

import java.util.List;

import javax.vecmath.Vector3f;

import main.Settings;
import main.TaskProvider;
import util.DistanceMatrix;
import util.ListUtil;
import alg.embed3d.Random3DEmbedder;
import alg.embed3d.ThreeDEmbedder;
import dataInterface.ClusterData;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

public class EmbedClusters
{
	Random3DEmbedder random = new Random3DEmbedder();

	public void embed(ThreeDEmbedder clusterEmbedder, DatasetFile dataset, ClusteringData clustering)
	{
		ThreeDEmbedder emb;
		if (clustering.getSize() == 1)
			emb = random;
		else
			emb = clusterEmbedder;
		emb = secureEmbed(emb, "cluster", clustering.getSize() + " clusters", dataset,
				ListUtil.cast(MolecularPropertyOwner.class, clustering.getClusters()), clustering.getFeatures(),
				emb.requiresDistances() ? clustering.getClusterDistances().cast(MolecularPropertyOwner.class) : null);

		int cCount = 0;
		for (Vector3f v : emb.getPositions())
			((ClusterDataImpl) clustering.getClusters().get(cCount++)).setPosition(v);

		cCount = 0;
		for (final ClusterData cluster : clustering.getClusters())
		{
			ClusterDataImpl c = (ClusterDataImpl) cluster;
			TaskProvider.task().update(
					"Embedding compounds of cluster " + (cCount + 1) + "/" + clustering.getClusters().size()
							+ " into 3D-space (num compounds: " + c.getSize() + ")");

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

			if (c.getSize() == 1)
				emb = random;
			else
				emb = clusterEmbedder;
			emb = secureEmbed(
					emb,
					"compound",
					"cluster " + (cCount + 1) + " (size: " + c.getSize() + ")",
					dataset,
					ListUtil.cast(MolecularPropertyOwner.class, c.getCompounds()),
					clustering.getFeatures(),
					emb.requiresDistances() ? c.getCompoundDistances(clustering.getFeatures()).cast(
							MolecularPropertyOwner.class) : null);

			int mCount = 0;
			for (Vector3f v : emb.getPositions())
				((CompoundDataImpl) c.getCompounds().get(mCount++)).setPosition(v);
			cCount++;
		}
	}

	public ThreeDEmbedder secureEmbed(ThreeDEmbedder emb, String embedItem, String embedItems, DatasetFile dataset,
			List<MolecularPropertyOwner> instances, List<MoleculeProperty> features,
			DistanceMatrix<MolecularPropertyOwner> distances)
	{
		try
		{
			emb.embed(dataset, instances, features, distances);
			return emb;
		}
		catch (Exception e)
		{
			if (emb == random)
			{
				e.printStackTrace();
				throw new Error("random embedder should not fail!");
			}

			if (e.getMessage().contains("No attributes!"))
				TaskProvider.task().warning(
						"Could not embedd " + embedItems + ", as every " + embedItem
								+ " has equal feature values, using random positions",
						"3D-Embedding uses feature values to embedd compounds in 3D-space, with similar " + embedItem
								+ "s close to each other. In " + embedItems + " all " + embedItem
								+ "s have equal feature values, and cannot be distinguished by the embedder.");
			else
				TaskProvider.task().warning(
						emb.getName() + " failed on embedding " + embedItems + ", using random positions",
						e.getMessage());

			e.printStackTrace();
			try
			{
				random.embed(dataset, instances, features, distances);
				return random;
			}
			catch (Exception e2)
			{
				e2.printStackTrace();
				throw new Error("random embedder should not fail!");
			}
		}
	}
}
