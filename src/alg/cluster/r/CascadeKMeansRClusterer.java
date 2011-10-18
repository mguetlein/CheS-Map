package alg.cluster.r;

import gui.property.IntegerProperty;
import gui.property.Property;
import gui.property.SelectProperty;

import java.util.HashMap;

import main.Settings;
import rscript.RScriptUtil;

class CascadeKMeansRClusterer extends AbstractRClusterer
{

	@Override
	public String getName()
	{
		return "k-Means - Cascade (R)";
	}

	@Override
	public String getDescription()
	{
		return "Uses "
				+ Settings.R_STRING
				+ ".\n"
				+ "Runs k-Means algorithm for different sizes of k. Selects the best clustering result according to one of two available criterias.\n"
				+ "Details: http://cc.oulu.fi/~jarioksa/softhelp/vegan/html/cascadeKM.html";
	}

	@Override
	protected String getRScriptName()
	{
		return "cascadeKM_" + minK.getValue() + "_" + maxK.getValue() + "_" + restart.getValue();
	}

	@Override
	protected String getRScriptCode()
	{
		String s = "args <- commandArgs(TRUE)\n";
		s += RScriptUtil.installAndLoadPackage("vegan");
		s += "df = read.table(args[1])\n";
		s += "if(" + maxK.getValue() + " < " + minK.getValue() + ") stop(\"min > max\")\n";
		s += "maxK <- min(" + maxK.getValue() + ",nrow(unique(df)))\n";
		s += "if(maxK < " + minK.getValue() + ") stop(\"min > num unique data points\")\n";
		s += "print(maxK)\n";
		s += "ccas <- cascadeKM(df, " + minK.getValue() + ", maxK, iter = " + restart.getValue() + ", criterion = \""
				+ critMap.get(criterion.getValue().toString()) + "\")\n";
		s += "max <- max.col(ccas$results)[2]\n";
		s += "print(ccas$results)\n";
		s += "print(ccas$partition[,max])\n";
		s += "write.table(ccas$partition[,max],args[2])\n";
		return s;
	}

	private static final String calinski = "Calinski-Harabasz (1974) criterion (calinski)";
	private static final String ssi = "simple structure index (ssi)";
	public static final HashMap<String, String> critMap = new HashMap<String, String>();
	static
	{
		critMap.put(calinski, "calinski");
		critMap.put(ssi, "ssi");
	}

	IntegerProperty minK = new IntegerProperty("minimum number of clusters (inf.gr)", 2);
	IntegerProperty maxK = new IntegerProperty("maximum number of clusters (sup.gr)", 15);
	IntegerProperty restart = new IntegerProperty("number of restarts (iter)", 30);
	SelectProperty criterion = new SelectProperty("criterion to select the best partition (criterion)", new String[] {
			calinski, ssi }, calinski);

	@Override
	public Property[] getProperties()
	{
		return new Property[] { minK, maxK, restart, criterion };
	}

}