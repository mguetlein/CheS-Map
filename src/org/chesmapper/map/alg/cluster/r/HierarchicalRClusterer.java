package org.chesmapper.map.alg.cluster.r;

import org.chesmapper.map.alg.DistanceMeasure;
import org.chesmapper.map.alg.cluster.ClusterApproach;
import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.alg.r.DistanceProperty;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.gui.FeatureWizardPanel.FeatureInfo;
import org.chesmapper.map.main.Settings;
import org.mg.javalib.gui.Messages;
import org.mg.javalib.gui.property.IntegerProperty;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.gui.property.SelectProperty;

import weka.clusterers.HierarchicalClusterer;

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
		return Settings.text("cluster.r.hierarchical.desc", Settings.R_STRING) + "\n\n"
				+ Settings.text("distance.desc");
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
				+ distance.loadPackage() + "\n"//
				+ "df = read.table(args[1])\n" //
				+ "set.seed(1)\n" //
				+ "d <- " + distance.computeDistance("df") + "\n" //
				+ "fit <- hclust(d, method=\"" + method.getValue() + "\")\n" //
				+ "\ngroups <- cutree(fit, k=" + k.getValue() + ")\n" //
				+ "print(groups)\n" //
				+ "write.table(groups,args[2])\n";
	}

	DistanceProperty distance = new DistanceProperty(getName());
	IntegerProperty k = new IntegerProperty("Number of clusters (k)", HierarchicalRClusterer.class.getName() + "_k", 5);
	SelectProperty method = new SelectProperty("The agglomeration method to be used (method)",
			HierarchicalClusterer.class.getName() + "_method", methods, methods[0]);

	@Override
	public Property[] getProperties()
	{
		return new Property[] { distance, k, method };
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

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		distance.addWarning(m, featureInfo);
		return m;
	}
}