package org.chesmapper.map.alg.cluster.r;

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
import org.mg.javalib.util.StringLineAdder;

public class DynamicTreeCutHierarchicalRClusterer extends AbstractRClusterer
{
	public static final String[] methods = { "ward", "single", "complete", "average", "mcquitty", "median", "centroid" };
	public static final DynamicTreeCutHierarchicalRClusterer INSTANCE = new DynamicTreeCutHierarchicalRClusterer();

	private DynamicTreeCutHierarchicalRClusterer()
	{
		clusterApproach = ClusterApproach.Connectivity;
	}

	@Override
	public String getName()
	{
		return Settings.text("cluster.r.dynamic-hierarchical");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("cluster.r.dynamic-hierarchical.desc", Settings.R_STRING) + "\n\n"
				+ Settings.text("distance.desc");
	}

	@Override
	protected String getShortName()
	{
		return "dyncut";
	}

	@Override
	protected String getRScriptCode()
	{
		StringLineAdder s = new StringLineAdder();
		s.add("args <- commandArgs(TRUE)");
		s.add(RScriptUtil.installAndLoadPackage("dynamicTreeCut"));
		s.add(distance.loadPackage());
		s.add("df = read.table(args[1])");
		s.add("d <- " + distance.computeDistance("df"));
		s.add("fit <- hclust(d, method=\"" + method.getValue() + "\")");
		s.add("dm <- as.matrix(d)");
		s.add("dm[is.na(dm)] = 0");
		s.add("cut = cutreeDynamic(fit, method = \"hybrid\", distM = dm, minClusterSize = " + minClusterSize.getValue()
				+ ")");
		s.add("print(cut)");
		s.add("write.table(cut,args[2])");
		return s.toString();
	}

	DistanceProperty distance = new DistanceProperty(getName());
	IntegerProperty minClusterSize = new IntegerProperty(
			"Minimum number of compounds in each cluster (minClusterSize)", 10);
	SelectProperty method = new SelectProperty("The agglomeration method to be used (method)", methods, methods[0]);

	@Override
	public Property[] getProperties()
	{
		return new Property[] { distance, minClusterSize, method };
	}

	@Override
	public DistanceMeasure getDistanceMeasure()
	{
		return distance.getDistanceMeasure();
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		distance.addWarning(m, featureInfo);
		return m;
	}
}