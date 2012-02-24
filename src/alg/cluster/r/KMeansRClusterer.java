package alg.cluster.r;

import gui.property.IntegerProperty;
import gui.property.Property;
import main.Settings;
import alg.cluster.ClusterApproach;

public class KMeansRClusterer extends AbstractRClusterer
{
	public static final KMeansRClusterer INSTANCE = new KMeansRClusterer();

	private KMeansRClusterer()
	{
		clusterApproach = ClusterApproach.Centroid;
	}

	public static String getNameStatic()
	{
		return Settings.text("cluster.r.kmeans");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("cluster.r.kmeans.desc", Settings.R_STRING);
	}

	@Override
	public String getName()
	{
		return getNameStatic();
	}

	@Override
	protected String getShortName()
	{
		return "kmeans";
	}

	@Override
	protected String getRScriptCode()
	{
		return "args <- commandArgs(TRUE)\n" //
				+ "df = read.table(args[1])\n" //
				+ "if(" + k.getValue() + " > nrow(unique(df))) stop(\"" + TOO_FEW_UNIQUE_DATA_POINTS + "\")\n" //
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
	public Property getRandomRestartProperty()
	{
		return restart;
	}

	@Override
	public Property getFixedNumClustersProperty()
	{
		return k;
	}
}