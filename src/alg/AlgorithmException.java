package alg;

import alg.cluster.DatasetClusterer;
import alg.embed3d.ThreeDEmbedder;

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
