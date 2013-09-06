package alg.cluster.r;

import gui.property.IntegerProperty;
import gui.property.Property;
import main.Settings;
import alg.DistanceMeasure;
import alg.cluster.ClusterApproach;
import alg.r.DistanceProperty;

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
				+ distance.loadPackage() + "\n"//
				+ "df = read.table(args[1])\n" //
				+ "print(paste('unique ',nrow(unique(df))))\n" //
				+ "if(" + k.getValue() + " > nrow(unique(df))) stop(\"" + TOO_FEW_UNIQUE_DATA_POINTS + "\")\n" //
				+ "d <- " + distance.computeDistance("df") + "\n" //
				+ "dm <- as.matrix(d)\n" //
				+ "dm[is.na(dm)] = 0\n"//
				+ "res <- kmeans(dm, " + k.getValue() + ",nstart=" + restart.getValue() + ")\n" //
				+ "print(res$cluster)\n" //
				+ "print(res$withinss)\n" //
				+ "write.table(res$cluster,args[2])\n";
	}

	DistanceProperty distance = new DistanceProperty(getName());
	IntegerProperty k = new IntegerProperty("Number of clusters (k)", 5);
	IntegerProperty restart = new IntegerProperty("Number of restarts (nstart)", 10);

	@Override
	public Property[] getProperties()
	{
		return new Property[] { distance, k, restart };
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

	@Override
	public DistanceMeasure getDistanceMeasure()
	{
		return distance.getDistanceMeasure();
	}
}