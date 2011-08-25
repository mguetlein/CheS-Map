package alg.embed3d;

import gui.property.Property;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import main.Settings;
import util.DistanceMatrix;
import util.Vector3fUtil;
import data.DatasetFile;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

public class Random3DEmbedder implements ThreeDEmbedder
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
	public String getPreconditionErrors()
	{
		return null;
	}

	List<Vector3f> positions;

	@Override
	public void embed(DatasetFile dataset, List<MolecularPropertyOwner> instances, List<MoleculeProperty> features,
			DistanceMatrix<MolecularPropertyOwner> distances)
	{
		positions = getPositions(instances.size());
	}

	@Override
	public List<Vector3f> getPositions()
	{
		return positions;
	}

	@Override
	public Property[] getProperties()
	{
		return new Property[0];
	}

	@Override
	public void setProperties(Property[] properties)
	{

	}

	@Override
	public String getName()
	{
		return "No 3D-Embedding (Random positions)";
	}

	@Override
	public String getDescription()
	{
		return "The compound features are ignored. This compounds are arranged randomly, equally distributed in a sphere.";
	}

	@Override
	public boolean requiresDistances()
	{
		return false;
	}

	@Override
	public boolean requiresFeatures()
	{
		return false;
	}

}
