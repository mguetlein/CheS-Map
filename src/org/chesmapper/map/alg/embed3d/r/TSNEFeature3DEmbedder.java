package org.chesmapper.map.alg.embed3d.r;

import org.chesmapper.map.alg.DistanceMeasure;
import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.alg.embed3d.AbstractRTo3DEmbedder;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.gui.FeatureWizardPanel.FeatureInfo;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.rscript.RScriptUtil;
import org.chesmapper.map.util.MessageUtil;
import org.mg.javalib.gui.Messages;
import org.mg.javalib.gui.property.IntegerProperty;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.util.StringLineAdder;

public class TSNEFeature3DEmbedder extends AbstractRTo3DEmbedder
{
	public static final TSNEFeature3DEmbedder INSTANCE = new TSNEFeature3DEmbedder();

	private TSNEFeature3DEmbedder()
	{
	}

	public int getMinNumFeatures()
	{
		return 2;
	}

	@Override
	public int getMinNumInstances()
	{
		return 3;
	}

	@Override
	public String getShortName()
	{
		return "tsne";
	}

	@Override
	public String getName()
	{
		return Settings.text("embed.r.tsne");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("embed.r.tsne.desc", Settings.R_STRING);
	}

	@Override
	public Property getRandomSeedProperty()
	{
		return randomSeed;
	}

	IntegerProperty maxNumIterations = new IntegerProperty("Maximum number of iterations (max_iter)", 1000);
	IntegerProperty perplexity = new IntegerProperty("Optimal number of neighbors (perplexity)", 30);
	IntegerProperty initial_dims = new IntegerProperty(
			"The number of dimensions to use in reduction method (initial_dims)", 30);
	IntegerProperty randomSeed = new IntegerProperty("Random seed", "Tsne random seed", 1);

	@Override
	public Property[] getProperties()
	{
		return new Property[] { maxNumIterations, perplexity, initial_dims, randomSeed };
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		if (featureInfo.isNumPairsHigh())
			m.add(MessageUtil.slowRuntimeMessage(featureInfo.getNumPairsWarning()));
		return m;
	}

	@Override
	protected String getErrorDescription(String errorOut)
	{
		if (errorOut.contains("number of unique data points < 3"))
			return TOO_FEW_UNIQUE_DATA_POINTS;
		if (errorOut.contains("while (abs(Hdiff) > tol && tries < 50)"))
			return "t-SNE tends to fail with improper input data (especially on small datasets). Try a different embedding algorithm.";
		else
			return null;
	}

	@Override
	protected String getRScriptCode()
	{
		StringLineAdder s = new StringLineAdder();
		s.add("args <- commandArgs(TRUE)");
		s.add(RScriptUtil.installAndLoadPackage("tsne"));
		s.add("df = read.table(args[1])");
		s.add("set.seed(" + randomSeed.getValue() + ")");
		s.add("inst <- nrow(unique(df))");
		s.add("print(paste('unique instances ',inst))");
		s.add("if(inst < 3) stop(\"number of unique data points < 3\")");
		s.add("perp <- min(" + perplexity.getValue() + ",inst)");
		s.add("feats <- ncol(df)");
		s.add("print(paste('features ',feats))");
		s.add("dims <- min(" + initial_dims.getValue() + ",feats)");
		s.add("res <- tsne(df, k = 3, perplexity=perp, max_iter=" + maxNumIterations.getValue()
				+ ", initial_dims=dims)");
		s.add("print(head(res$ydata))");
		//+ "##res <- smacofSphere.dual(df, ndim = 3)\n" + "#print(res$conf)\n" + "#print(class(res$conf))\n"
		s.add("write.table(res$ydata,args[2])");
		s.add("write.table(as.matrix(dist(df, method = \"euclidean\")),args[3])");
		return s.toString();
	}

	@Override
	public boolean isLinear()
	{
		return false;
	}

	@Override
	public boolean isLocalMapping()
	{
		return true;
	}

	@Override
	public DistanceMeasure getDistanceMeasure()
	{
		return DistanceMeasure.EUCLIDEAN_DISTANCE;
	}
}
