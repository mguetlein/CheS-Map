package org.chesmapper.map.alg;

import org.chesmapper.map.alg.build3d.ThreeDBuilder;
import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.alg.embed3d.ThreeDEmbedder;

public class AlgorithmException extends RuntimeException
{
	Algorithm a;

	private AlgorithmException(Algorithm a, String msg)
	{
		super(msg);
		this.a = a;
	}

	private AlgorithmException(Algorithm a, Exception e)
	{
		super(e);
		this.a = a;
	}

	public Algorithm getAlgorithm()
	{
		return a;
	}

	public static class ThreeDBuilderException extends AlgorithmException
	{
		public ThreeDBuilderException(ThreeDBuilder c, String msg)
		{
			super(c, msg);
		}
	}

	public static class ClusterException extends AlgorithmException
	{
		public ClusterException(DatasetClusterer c, String msg)
		{
			super(c, msg);
		}
	}

	public static class EmbedException extends AlgorithmException
	{
		public EmbedException(ThreeDEmbedder c, String msg)
		{
			super(c, msg);
		}
	}
}
