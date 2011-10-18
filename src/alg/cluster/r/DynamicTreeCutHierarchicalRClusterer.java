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
		//return "Dynamic Tree Cut Hierachical Clusterer (R)";
		return "Hierachical - Dynamic Tree Cut (R)";
	}

	@Override
	public String getDescription()
	{
		return "Uses "
				+ Settings.R_STRING
				+ ".\n"
				+ "Automatically detects clusters in the dendogram produced by hierachical clustering.\n"
				+ "Details: http://www.genetics.ucla.edu/labs/horvath/CoexpressionNetwork/BranchCutting (The <i>hybrid</i> method is used that takes the distance matrix and the dendogramm into account.)";
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

	@Override
	public String getFixedNumClustersProperty()
	{
		return null;
	}
}