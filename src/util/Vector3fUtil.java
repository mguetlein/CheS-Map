package util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Vector3f;

import main.Settings;

public class Vector3fUtil
{
	public static String serialize(Vector3f v)
	{
		return v.x + "#" + v.y + "#" + v.z;
	}

	public static Vector3f deserialize(String s)
	{
		String ss[] = s.split("#");
		return new Vector3f(Float.parseFloat(ss[0]), Float.parseFloat(ss[1]), Float.parseFloat(ss[2]));
	}

	/**
	 * trys to parse String array into Vector3f array (empty strings/null are inserted as null)
	 * returns null if a non-parsable String is included
	 * 
	 * @param array
	 * @return
	 */
	public static Vector3f[] parseArray(Object[] array)
	{
		Vector3f vectors[] = new Vector3f[array.length];
		for (int i = 0; i < vectors.length; i++)
		{
			if (array[i] == null || array[i].equals("null") || array[i].toString().trim().length() == 0)
				vectors[i] = null;
			else
			{
				try
				{
					vectors[i] = deserialize(array[i].toString());
				}
				catch (Exception e)
				{
					return null;
				}
			}
		}
		return vectors;
	}

	public static String toString(Vector3f v)
	{
		return "{" + v.x + " " + v.y + " " + v.z + "}";
	}

	public static String toNiceString(Vector3f v)
	{
		return "(" + StringUtil.formatDouble(v.x) + ", " + StringUtil.formatDouble(v.y) + ", "
				+ StringUtil.formatDouble(v.z) + ")";
	}

	public static float dist(Vector3f v1, Vector3f v2)
	{
		Vector3f v = new Vector3f(v1);
		v.sub(v2);
		return v.length();
	}

	public static Vector3f center(Vector3f[] vectors)
	{
		Vector3f v = new Vector3f(0, 0, 0);
		for (Vector3f vv : vectors)
			v.add(vv);
		v.scale(1 / (float) vectors.length);
		return v;
	}

	public static Vector3f centerBoundbox(Vector3f[] vectors)
	{
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for (Vector3f v : vectors)
		{
			minX = Math.min(minX, v.x);
			minY = Math.min(minY, v.y);
			minZ = Math.min(minZ, v.z);
			maxX = Math.max(maxX, v.x);
			maxY = Math.max(maxY, v.y);
			maxZ = Math.max(maxZ, v.z);
		}
		return new Vector3f((maxX + minX) / 2.0f, (maxY + minY) / 2.0f, (maxZ + minZ) / 2.0f);
	}

	/**
	 * this is still not the centroid (better then simple center or centerBoundBox) 
	 * 
	 * drift-two-top-left-example in 2d:
	 * .x..............
	 * x..............x
	 * ................
	 * ................ 
	 * .xxx............
	 * .xx.............
	 * .x..............
	 * 
	 * 
	 * @param vectors
	 * @return
	 */
	public static Vector3f centerConvexHull(Vector3f[] vectors)
	{
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for (Vector3f v : vectors)
		{
			minX = Math.min(minX, v.x);
			minY = Math.min(minY, v.y);
			minZ = Math.min(minZ, v.z);
			maxX = Math.max(maxX, v.x);
			maxY = Math.max(maxY, v.y);
			maxZ = Math.max(maxZ, v.z);
		}
		List<Vector3f> convexHull = new ArrayList<Vector3f>();
		for (Vector3f vv : vectors)
			if (!contains(convexHull, vv)
					&& (vv.x == minX || vv.x == maxX || vv.y == minY || vv.y == maxY || vv.z == minZ || vv.z == maxZ))
				convexHull.add(vv);
		Vector3f v = new Vector3f(0, 0, 0);
		for (Vector3f vector3f : convexHull)
			v.add(vector3f);
		v.scale(1 / (float) convexHull.size());
		return v;
	}

	/**
	 * Vector3f.equals does something else
	 * 
	 * @param vectors
	 * @param v
	 * @return
	 */
	public static boolean contains(List<Vector3f> vectors, Vector3f v)
	{
		for (Vector3f v1 : vectors)
			if (v1.x == v.x && v1.y == v.y && v1.z == v.z)
				return true;
		return false;
	}

	public static float avgMinDist(Vector3f[] vectors)
	{
		float dist = 0;
		for (int i = 0; i < vectors.length; i++)
		{
			float min = Float.MAX_VALUE;
			for (int j = 0; j < vectors.length; j++)
				if (i != j)
					min = Math.min(min, dist(vectors[i], vectors[j]));
			dist += min;
		}
		dist /= vectors.length;
		return dist;
	}

	public static float minDist(Vector3f[] vectors)
	{
		float min = Float.MAX_VALUE;
		for (int i = 0; i < vectors.length - 1; i++)
			for (int j = i + 1; j < vectors.length; j++)
				min = Math.min(min, dist(vectors[i], vectors[j]));
		return min;
	}

	public static float maxDist(Vector3f[] vectors)
	{
		float max = 0;
		for (int i = 0; i < vectors.length - 1; i++)
			for (int j = i + 1; j < vectors.length; j++)
				max = Math.max(max, dist(vectors[i], vectors[j]));
		return max;
	}

	public static Vector3f randomVector(float radius, Random random)
	{
		float max = radius;
		float x = random.nextFloat() * max * (random.nextBoolean() ? 1 : -1);

		max = (float) Math.sqrt(Math.pow(radius, 2) - Math.pow(x, 2));
		float y = random.nextFloat() * max * (random.nextBoolean() ? 1 : -1);

		max = (float) Math.sqrt(Math.pow(radius, 2) - (Math.pow(x, 2) + Math.pow(y, 2)));
		float z = random.nextFloat() * max * (random.nextBoolean() ? 1 : -1);

		float[] vec = new float[] { x, y, z };
		ArrayUtil.scramble(vec, random);
		return new Vector3f(vec);

		// float x, y, z;
		// Vector3f v;
		// while (true)
		// {
		// x = random.nextFloat() * radius * (random.nextBoolean() ? 1 : -1);
		// y = random.nextFloat() * radius * (random.nextBoolean() ? 1 : -1);
		// z = random.nextFloat() * radius * (random.nextBoolean() ? 1 : -1);
		// v = new Vector3f(x, y, z);
		//
		// if (v.length() < radius)
		// return v;
		// }
	}

	public static void main(String args[])
	{
		Settings.LOGGER.info(new Vector3f(2, 3, 4));

		//		Random random = new Random();
		//		Vector3f sum = null;
		//		long runs = 0;
		//
		//		while (runs < 100000)
		//		{
		//			Vector3f v = randomVector(1, random);
		//			// Settings.LOGGER.println(runs + "   v: " + v);
		//
		//			if (sum == null)
		//				sum = new Vector3f(v);
		//			else
		//				sum.add(v);
		//			runs++;
		//		}
		//		Vector3f avg = new Vector3f(sum);
		//		avg.scale(1 / (float) runs);
		//		Settings.LOGGER.println(runs + " avg: " + avg);
		//		Settings.LOGGER.println(runs + " length: " + avg.length());

	}
}
