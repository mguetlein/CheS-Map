package alg.embed3d;

import gui.property.Property;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import main.Settings;
import util.Vector3fUtil;
import data.DatasetFile;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

public class Random3DEmbedder extends Abstract3DEmbedder
{

	private static List<Vector3f> getPositions(int numPositions)
	{
		List<Vector3f> pos = new ArrayList<Vector3f>();
		for (int i = 0; i < numPositions; i++)
			pos.add(addRandomPosition(pos, 1));//, 0.9f));
		return pos;
	}

	private static Vector3f addRandomPosition(List<Vector3f> existing, float radius) //, float clusterRadius)
	{
		//		if (existing == null || existing.size() == 0)
		//			return new Vector3f(0, 0, 0);

		int count = 0;
		double dist = radius * 0.9;
		while (true)
		{
			Vector3f v = Vector3fUtil.randomVector(radius, Settings.RANDOM);
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
			if (count > 50 && count % 10 == 0)
			{
				//System.err.println("reduce radius " + count);
				dist *= 0.9;
			}
		}
	}

	@Override
	public void embed(DatasetFile dataset, List<MolecularPropertyOwner> instances, List<MoleculeProperty> features)
	{
		positions = getPositions(instances.size());
	}

	@Override
	public Property[] getProperties()
	{
		return new Property[0];
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

}
