package alg.cluster.r;

import gui.property.IntegerProperty;
import gui.property.Property;
import main.Settings;

public class KMeansRClusterer extends AbstractRClusterer
{
	public static String getNameStatic()
	{
		return "k-Means (R)";
	}

	@Override
	public String getName()
	{
		return getNameStatic();
	}

	@Override
	public String getDescription()
	{
		return "Uses " + Settings.R_STRING + ".\n" //
				+ "Assignes compounds to k randomly initialized centroids. " //
				+ "Iteratively updates centroid positions and re-assignes compounds until the algorithm converges.\n" //
				+ "Implementation details: " + "http://stat.ethz.ch/R-manual/R-patched/library/stats/html/kmeans.html";
	}

	@Override
	protected String getRScriptName()
	{
		return "kmeans_" + k.getValue() + "_" + restart.getValue();
	}

	@Override
	protected String getRScriptCode()
	{
		return "args <- commandArgs(TRUE)\n" //
				+ "df = read.table(args[1])\n" //
				+ "res <- kmeans(df, " + k.getValue() + ",nstart=" + restart.getValue() + ")\n" //
				+ "print(res$cluster)\n" //
				+ "print(res$withinss)\n" //
				+ "write.table(res$cluster,args[2])\n";
	}

	IntegerProperty k = new IntegerProperty("number of clusters (k)", 5);
	IntegerProperty restart = new IntegerProperty("number of restarts (nstart)", 10);

	@Override
	public Property[] getProperties()
	{
		return new Property[] { k, restart };
	}

	@Override
	public String getFixedNumClustersProperty()
	{
		return k.getName();
	}
}