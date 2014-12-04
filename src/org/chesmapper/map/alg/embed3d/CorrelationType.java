package org.chesmapper.map.alg.embed3d;

public enum CorrelationType
{
	RSquare, CCC, Pearson;

	public static CorrelationType[] types()
	{
		return new CorrelationType[] { Pearson };
	}
}
