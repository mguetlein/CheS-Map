package org.chesmapper.map.alg;

public class DistanceMeasure
{
	public boolean equals(Object o)
	{
		if (!(o instanceof DistanceMeasure))
			return false;
		if (this == UNKNOWN_DISTANCE || o == UNKNOWN_DISTANCE)
			return false;
		return this.toString().equals(o.toString());
	}

	String measure;

	public DistanceMeasure(String measure)
	{
		this.measure = measure;
	}

	@Override
	public String toString()
	{
		return measure;
	}

	public static DistanceMeasure EUCLIDEAN_DISTANCE = new DistanceMeasure("Euclidean");

	public static DistanceMeasure UNKNOWN_DISTANCE = new DistanceMeasure("N/A");
}
