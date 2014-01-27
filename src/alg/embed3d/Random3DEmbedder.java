package alg.embed3d;

import gui.property.IntegerProperty;
import gui.property.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Vector3f;

import main.Settings;
import util.Vector3fUtil;
import alg.AbstractAlgorithm;
import alg.DistanceMeasure;
import data.DatasetFile;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;

public class Random3DEmbedder extends AbstractAlgorithm implements ThreeDEmbedder
{
	public static Random3DEmbedder INSTANCE = new Random3DEmbedder();

	private Random3DEmbedder()
	{
	}

	protected double[][] distances;
	private List<Vector3f> positions;
	private double rSquare = -Double.MAX_VALUE;
	private double ccc = -Double.MAX_VALUE;

	private List<Vector3f> getPositions(int numPositions)
	{
		rand = new Random((long) randomSeed.getValue());
		dist = 0.95f;
		count = 0;
		List<Vector3f> pos = new ArrayList<Vector3f>();
		for (int i = 0; i < numPositions; i++)
			pos.add(addRandomPosition(pos));
		return pos;
	}

	@Override
	public double getRSquare()
	{
		return rSquare;
	}

	@Override
	public double getCCC()
	{
		return ccc;
	}

	@Override
	public CompoundProperty getCCCProperty()
	{
		return null;
	}

	//	@Override
	//	public CompoundPropertyEmbedQuality getEmbedQuality(CompoundProperty p, DatasetFile dataset,
	//			List<MolecularPropertyOwner> instances)
	//	{
	//		return new CompoundPropertyEmbedQuality(p, positions, instances, dataset);
	//	}

	IntegerProperty randomSeed = new IntegerProperty("Random seed", "Random embedding - Random seed", 1);

	private Random rand;
	float dist;
	int count;

	private Vector3f addRandomPosition(List<Vector3f> existing) //, float clusterRadius)
	{
		//		if (existing == null || existing.size() == 0)
		//			return new Vector3f(0, 0, 0);

		while (true)
		{
			Vector3f v = Vector3fUtil.randomVector(dist, rand);
			boolean centerTooClose = false;
			for (Vector3f v2 : existing)
			{
				if (Vector3fUtil.dist(v, v2) < dist)
				{
					centerTooClose = true;
					break;
				}
			}
			if (!centerTooClose)
				return v;
			count++;
			if (count % 50 == 0)
			{
				dist *= 0.9;
				//				Settings.LOGGER.warn(count + " reduce radius to " + dist);
			}
		}
	}

	@Override
	public double[][] getFeatureDistanceMatrix()
	{
		return distances;
	}

	@Override
	public void embedDataset(DatasetFile dataset, List<CompoundData> instances, List<CompoundProperty> features)
	{
		positions = getPositions(instances.size());
		distances = EmbedUtil.euclMatrix(instances, features, dataset);
		if (instances.size() > 2)
		{
			rSquare = EmbedUtil.computeRSquare(positions, distances);
			ccc = EmbedUtil.computeCCC(positions, distances);
		}
	}

	@Override
	public Property[] getProperties()
	{
		return new Property[] { randomSeed };
	}

	@Override
	public Property getRandomSeedProperty()
	{
		return randomSeed;
	}

	@Override
	public String getName()
	{
		return getNameStatic();
	}

	public static String getNameStatic()
	{
		return Settings.text("embed.random");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("embed.random.desc");
	}

	@Override
	public boolean requiresFeatures()
	{
		return false;
	}

	@Override
	public List<Vector3f> getPositions()
	{
		return positions;
	}

	@Override
	public boolean isLinear()
	{
		return false;
	}

	@Override
	public boolean isLocalMapping()
	{
		return false;
	}

	@Override
	public DistanceMeasure getDistanceMeasure()
	{
		return DistanceMeasure.UNKNOWN_DISTANCE;
	}
}
