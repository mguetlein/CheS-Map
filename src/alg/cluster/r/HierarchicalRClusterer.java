package alg.cluster.r;

import gui.property.IntegerProperty;
import gui.property.Property;
import gui.property.SelectProperty;
import main.Settings;
import weka.clusterers.HierarchicalClusterer;
import alg.cluster.ClusterApproach;

public class HierarchicalRClusterer extends AbstractRClusterer
{
	public static final String[] methods = { "ward", "single", "complete", "average", "mcquitty", "median", "centroid" };
	public static final HierarchicalRClusterer INSTANCE = new HierarchicalRClusterer();

	private HierarchicalRClusterer()
	{
		clusterApproach = ClusterApproach.Connectivity;
	}

	@Override
	public String getName()
	{
		return Settings.text("cluster.r.hierarchical");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("cluster.r.hierarchical.desc", Settings.R_STRING);
	}

	@Override
	protected String getShortName()
	{
		return "hclust";
	}

	@Override
	protected String getRScriptCode()
	{
		return "args <- commandArgs(TRUE)\n" //
				+ "df = read.table(args[1])\n" //
				+ "set.seed(1)\n" //
				+ "d <- dist(df, method = \"euclidean\")\n" //
				+ "fit <- hclust(d, method=\"" + method.getValue() + "\")\n" //
				+ "\ngroups <- cutree(fit, k=" + k.getValue() + ")\n" //
				+ "print(groups)\n" //
				+ "write.table(groups,args[2])\n";
	}

	IntegerProperty k = new IntegerProperty("number of clusters (k)", HierarchicalRClusterer.class.getName() + "_k", 5);
	SelectProperty method = new SelectProperty("the agglomeration method to be used (method)",
			HierarchicalClusterer.class.getName() + "_method", methods, methods[0]);

	@Override
	public Property[] getProperties()
	{
		return new Property[] { k, method };
	}

	@Override
	public Property getFixedNumClustersProperty()
	{
		return k;
	}
}