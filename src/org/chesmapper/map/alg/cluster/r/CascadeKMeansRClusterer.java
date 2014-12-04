package org.chesmapper.map.alg.cluster.r;

import java.util.HashMap;

import org.chesmapper.map.alg.DistanceMeasure;
import org.chesmapper.map.alg.cluster.ClusterApproach;
import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.alg.r.DistanceProperty;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.gui.FeatureWizardPanel.FeatureInfo;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.rscript.RScriptUtil;
import org.mg.javalib.gui.Messages;
import org.mg.javalib.gui.property.IntegerProperty;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.gui.property.SelectProperty;

public class CascadeKMeansRClusterer extends AbstractRClusterer
{
	public static final CascadeKMeansRClusterer INSTANCE = new CascadeKMeansRClusterer();

	private CascadeKMeansRClusterer()
	{
		clusterApproach = ClusterApproach.Centroid;
	}

	@Override
	public String getName()
	{
		return Settings.text("cluster.r.cascade-kmeans");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("cluster.r.cascade-kmeans.desc", Settings.R_STRING) + "\n\n"
				+ Settings.text("distance.desc");
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
		s += distance.loadPackage() + "\n";
		s += "df = read.table(args[1])\n";
		s += "if(" + maxK.getValue() + " < " + minK.getValue() + ") stop(\"min > max\")\n";
		s += "maxK <- min(" + maxK.getValue() + ",nrow(unique(df)))\n";
		s += "maxK <- min(maxK,nrow(df)-1)\n"; // somehow the maximum value for maxK is n-1 for cascade 
		s += "if(maxK < " + minK.getValue() + ") stop(\"" + TOO_FEW_UNIQUE_DATA_POINTS + "\")\n";
		s += "print(maxK)\n";
		s += "d <- " + distance.computeDistance("df") + "\n";
		s += "dm <- as.matrix(d)\n";
		s += "dm[is.na(dm)] = 0\n";
		s += "ccas <- cascadeKM(dm, " + minK.getValue() + ", maxK, iter = " + restart.getValue() + ", criterion = \""
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

	DistanceProperty distance = new DistanceProperty(getName());
	IntegerProperty minK = new IntegerProperty("Minimum number of clusters (inf.gr)", 2);
	IntegerProperty maxK = new IntegerProperty("Maximum number of clusters (sup.gr)", 15);
	IntegerProperty restart = new IntegerProperty("Number of restarts (iter)", 30);
	SelectProperty criterion = new SelectProperty("Criterion to select the best partition (criterion)", new String[] {
			calinski, ssi }, calinski);

	@Override
	public Property[] getProperties()
	{
		return new Property[] { distance, minK, maxK, restart, criterion };
	}

	@Override
	public Property getRandomRestartProperty()
	{
		return restart;
	}

	public int getMinK()
	{
		return minK.getMinValue();
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		distance.addWarning(m, featureInfo);
		return m;
	}

	@Override
	public DistanceMeasure getDistanceMeasure()
	{
		return distance.getDistanceMeasure();
	}
}