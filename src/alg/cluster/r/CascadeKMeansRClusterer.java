package alg.cluster.r;

import gui.property.IntegerProperty;
import gui.property.Property;
import gui.property.SelectProperty;

import java.util.HashMap;

import main.Settings;
import rscript.RScriptUtil;

public class CascadeKMeansRClusterer extends AbstractRClusterer
{

	@Override
	public String getName()
	{
		return Settings.text("cluster.r.cascade-kmeans");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("cluster.r.cascade-kmeans.desc", Settings.R_STRING);
	}

	@Override
	protected String getShortName()
	{
		return "cascadeKM";
	}

	@Override
	protected String getRScriptCode()
	{
		String s = "args <- commandArgs(TRUE)\n";
		s += RScriptUtil.installAndLoadPackage("vegan");
		s += "df = read.table(args[1])\n";
		s += "if(" + maxK.getValue() + " < " + minK.getValue() + ") stop(\"min > max\")\n";
		s += "maxK <- min(" + maxK.getValue() + ",nrow(unique(df)))\n";
		s += "maxK <- min(maxK,nrow(df)-1)\n"; // somehow the maximum value for maxK is n-1 for cascade 
		s += "if(maxK < " + minK.getValue() + ") stop(\"" + TOO_FEW_UNIQUE_DATA_POINTS + "\")\n";
		s += "print(maxK)\n";
		s += "set.seed(1)\n";
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

	public int getMinK()
	{
		return minK.getMinValue();
	}

}