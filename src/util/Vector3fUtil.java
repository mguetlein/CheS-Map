package util;

import java.util.Random;

import javax.vecmath.Vector3f;

public class Vector3fUtil
{
	public static String toString(Vector3f v)
	{
		return "{" + v.x + " " + v.y + " " + v.z + "}";
	}

	public static float dist(Vector3f v1, Vector3f v2)
	{
		Vector3f v = new Vector3f(v1);
		v.sub(v2);
		return v.length();
	}

	public static Vector3f center(Vector3f[] vectors)
	{
		int vCount = 0;
		Vector3f v = new Vector3f(0, 0, 0);
		for (Vector3f vv : vectors)
		{
			v.add(vv);
			vCount++;
		}
		v.scale(1 / (float) vCount);
		return v;
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
		Random random = new Random();
		Vector3f sum = null;
		long runs = 0;

		while (runs < 100000)
		{
			Vector3f v = randomVector(1, random);
			// System.out.println(runs + "   v: " + v);

			if (sum == null)
				sum = new Vector3f(v);
			else
				sum.add(v);
			runs++;
		}
		Vector3f avg = new Vector3f(sum);
		avg.scale(1 / (float) runs);
		System.out.println(runs + " avg: " + avg);
		System.out.println(runs + " length: " + avg.length());

	}
}
