package org.chesmapper.map.alg.embed3d;

import java.util.Arrays;

import org.chesmapper.map.alg.DistanceMeasure;
import org.mg.javalib.util.ArrayUtil;

public class DistanceMatrix
{
	DistanceMeasure distanceMeasure;
	double[][] values;
	double[][] normalizedValues;

	public DistanceMatrix(DistanceMeasure distanceMeasure, double[][] values)
	{
		this.distanceMeasure = distanceMeasure;
		this.values = values;
	}

	public DistanceMeasure getDistanceMeasure()
	{
		return distanceMeasure;
	}

	public double[][] getValues()
	{
		return values;
	}

	public double[][] getNormalizedValues()
	{
		if (normalizedValues == null)
		{
			double[][] d = new double[values.length][values.length];
			for (int i = 0; i < d.length; i++)
				d[i] = Arrays.copyOf(values[i], values[i].length);
			for (int i = 0; i < d.length; i++)
				d[i][i] = d[0][1];
			ArrayUtil.normalize(d);
			for (int i = 0; i < d.length; i++)
				d[i][i] = 0.0;
			normalizedValues = d;
		}
		return normalizedValues;
	}
}
