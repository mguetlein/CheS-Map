package org.chesmapper.map.alg.embed3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Vector3f;

import org.chesmapper.map.alg.AbstractAlgorithm;
import org.chesmapper.map.alg.DistanceMeasure;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.main.Settings;
import org.mg.javalib.gui.property.IntegerProperty;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.util.Vector3fUtil;

public class Random3DEmbedder extends AbstractAlgorithm implements ThreeDEmbedder
{
	public static Random3DEmbedder INSTANCE = new Random3DEmbedder();

	private Random3DEmbedder()
	{
	}

	private List<Vector3f> positions;

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

	//	@Override
	//	public double getRSquare()
	//	{
	//		return Double.NaN;
	//	}

	@Override
	public double getCorrelation(CorrelationType t)
	{
		return Double.NaN;
	}

	@Override
	public CorrelationProperty getCorrelationProperty(CorrelationType t)
	{
		return null;
	}

	IntegerProperty randomSeed = new IntegerProperty("Random seed", "Random embedding - Random seed", 1);

	private Random rand;
	float dist;
	int count;

	private Vector3f addRandomPosition(List<Vector3f> existing)
	{
		while (true)
		{
			Vector3f v = Vector3fUtil.randomVector(1.0f, rand);
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
	public DistanceMatrix getFeatureDistanceMatrix()
	{
		return null;
	}

	@Override
	public void embedDataset(DatasetFile dataset, List<CompoundData> instances, List<CompoundProperty> features) //boolean[] trainInstances
	{
		positions = getPositions(instances.size());
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
