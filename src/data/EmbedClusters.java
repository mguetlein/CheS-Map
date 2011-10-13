package data;

import java.util.List;

import javax.vecmath.Vector3f;

import main.TaskProvider;
import util.DistanceMatrix;
import util.ListUtil;
import util.Vector3fUtil;
import alg.embed3d.Random3DEmbedder;
import alg.embed3d.ThreeDEmbedder;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

public class EmbedClusters
{
	Random3DEmbedder random = new Random3DEmbedder();

	public void embed(ThreeDEmbedder clusterEmbedder, DatasetFile dataset, ClusteringData clustering)
	{
		ThreeDEmbedder emb;
		if (dataset.numCompounds() == 1)
			emb = random;
		else
			emb = clusterEmbedder;

		//		List<IDFeature> idFeatures = new ArrayList<EmbedClusters.IDFeature>();
		//		AbstractMoleculeProperty.clearPropertyOfType(IDFeature.class);
		//		Random r = new Random();
		//		for (CompoundData c : clustering.getCompounds())
		//		{
		//			IDFeature id = new IDFeature("ID compound " + c.getIndex());
		//			//			String v[] = new String[dataset.numCompounds()];
		//			//			Arrays.fill(v, "0");
		//			//			v[c.getIndex()] = "1";
		//			for (CompoundData cc : clustering.getCompounds())
		//				//((CompoundDataImpl) cc).setStringValue(id, r.nextBoolean() ? "1" : "0");
		//				((CompoundDataImpl) cc).setStringValue(id, c == cc ? "1" : "0");
		//			//			id.setStringValues(dataset, v);
		//			idFeatures.add(id);
		//		}
		//		@SuppressWarnings("unchecked")
		//		List<MoleculeProperty> features = ListUtil.concat(
		//				ListUtil.cast(MoleculeProperty.class, clustering.getFeatures()),
		//				ListUtil.cast(MoleculeProperty.class, idFeatures));
		List<MoleculeProperty> features = clustering.getFeatures();

		emb = secureEmbed(emb, "compound", dataset.numCompounds() + " compounds", dataset,
				ListUtil.cast(MolecularPropertyOwner.class, clustering.getCompounds()), features,
				emb.requiresDistances() ? clustering.getCompoundDistances().cast(MolecularPropertyOwner.class) : null,
				null);

		int cCount = 0;
		for (Vector3f v : emb.getPositions())
			((CompoundDataImpl) clustering.getCompounds().get(cCount++)).setPosition(v);

		for (final ClusterData cluster : clustering.getClusters())
		{
			Vector3f positions[] = new Vector3f[cluster.getSize()];
			cCount = 0;
			for (CompoundData c : cluster.getCompounds())
				positions[cCount++] = c.getPosition();
			((ClusterDataImpl) cluster).setPosition(Vector3fUtil.center(positions));
		}
	}

	private ThreeDEmbedder secureEmbed(ThreeDEmbedder emb, String embedItem, String embedItems, DatasetFile dataset,
			List<MolecularPropertyOwner> instances, List<MoleculeProperty> features,
			DistanceMatrix<MolecularPropertyOwner> distances, ClusterDataImpl c)
	{
		try
		{
			emb.embed(dataset, instances, features, distances);
			return emb;
		}
		catch (Exception e)
		{
			e.printStackTrace();

			if (emb == random)
			{
				throw new Error("random embedder should not fail!");
			}

			if (e.getMessage().contains("No attributes!"))
				TaskProvider.task().warning(
						"Could not embedd " + embedItems + ", as every " + embedItem
								+ " has equal feature values, using random positions",
						"3D Embedding uses feature values to embedd compounds in 3D space, with similar " + embedItem
								+ "s close to each other. In " + embedItems + " all " + embedItem
								+ "s have equal feature values, and cannot be distinguished by the embedder.");
			else
				TaskProvider.task().warning(
						emb.getName() + " failed on embedding " + embedItems + ", using random positions",
						e.getMessage());

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

	//	static class IDFeature extends AbstractMoleculeProperty implements MoleculePropertySet
	//	{
	//
	//		public IDFeature(String name)
	//		{
	//			super(name, null);
	//			setType(Type.NOMINAL);
	//			setNominalDomain(new Object[] { "0", "1" });
	//		}
	//
	//		@Override
	//		public MoleculePropertySet getMoleculePropertySet()
	//		{
	//			return this;
	//		}
	//
	//		@Override
	//		public boolean isComputed(DatasetFile dataset)
	//		{
	//			return true;
	//		}
	//
	//		@Override
	//		public boolean compute(DatasetFile dataset)
	//		{
	//			return true;
	//		}
	//
	//		@Override
	//		public boolean isSizeDynamic()
	//		{
	//			return false;
	//		}
	//
	//		@Override
	//		public int getSize(DatasetFile d)
	//		{
	//			return 1;
	//		}
	//
	//		@Override
	//		public MoleculeProperty get(DatasetFile d, int index)
	//		{
	//			if (index > 0)
	//				throw new IllegalSelectorException();
	//			return this;
	//		}
	//
	//		@Override
	//		public Binary getBinary()
	//		{
	//			return null;
	//		}
	//
	//		@Override
	//		public boolean isUsedForMapping()
	//		{
	//			return true;
	//		}
	//	}

}
