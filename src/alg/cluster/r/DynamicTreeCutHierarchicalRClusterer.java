package alg.cluster.r;

import gui.property.IntegerProperty;
import gui.property.Property;
import gui.property.SelectProperty;
import main.Settings;
import rscript.RScriptUtil;

class DynamicTreeCutHierarchicalRClusterer extends AbstractRClusterer
{

	@Override
	public String getName()
	{
		return Settings.text("cluster.r.dynamic-hierarchical");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("cluster.r.dynamic-hierarchical.desc", Settings.R_STRING);
	}

	@Override
	protected String getRScriptName()
	{
		return "dyncut";
	}

	@Override
	protected String getRScriptCode()
	{
		return "args <- commandArgs(TRUE)\n" //
				+ RScriptUtil.installAndLoadPackage("dynamicTreeCut")//
				+ "df = read.table(args[1])\n" //
				+ "d <- dist(df, method = \"euclidean\")\n" //
				+ "fit <- hclust(d, method=\"" + method.getValue()
				+ "\")\n" //
				+ "cut = cutreeDynamic(fit, method = \"hybrid\", distM = as.matrix(d), minClusterSize = "
				+ minClusterSize.getValue() + ")\n" //
				+ "print(cut)\n" //
				+ "write.table(cut,args[2])\n";
	}

	IntegerProperty minClusterSize = new IntegerProperty(
			"minimum number of compounds in each cluster (minClusterSize)", 10);

	public static final String[] methods = { "ward", "single", "complete", "average", "mcquitty", "median", "centroid" };
	SelectProperty method = new SelectProperty("the agglomeration method to be used (method)", methods, methods[0]);

	@Override
	public Property[] getProperties()
	{
		return new Property[] { minClusterSize, method };
	}

}