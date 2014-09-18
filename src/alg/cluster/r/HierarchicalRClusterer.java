package alg.cluster.r;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Messages;
import gui.property.IntegerProperty;
import gui.property.Property;
import gui.property.SelectProperty;
import main.Settings;
import weka.clusterers.HierarchicalClusterer;
import alg.DistanceMeasure;
import alg.cluster.ClusterApproach;
import alg.cluster.DatasetClusterer;
import alg.r.DistanceProperty;
import data.DatasetFile;

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