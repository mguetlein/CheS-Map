package alg.embed3d.r;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Messages;
import gui.property.IntegerProperty;
import gui.property.Property;
import main.Settings;
import rscript.RScriptUtil;
import util.MessageUtil;
import alg.cluster.DatasetClusterer;
import alg.embed3d.AbstractRTo3DEmbedder;
import data.DatasetFile;

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
		return 2;
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

	private int getPerplexity()
	{
		if (numInstances == -1)
			throw new IllegalStateException("num instances not set before");
		return Math.max(2, Math.min(perplexity.getValue(), (int) numInstances));
	}

	private int getInitialDims()
	{
		if (numFeatures == -1)
			throw new IllegalStateException("num features not set before");
		return Math.max(2, Math.min(initial_dims.getValue(), (int) numFeatures));
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
		if (dataset.numCompounds() >= 50 && featureInfo.isNumFeaturesHigh())
			m.add(MessageUtil.slowMessage(featureInfo.getNumFeaturesWarning()));
		return m;
	}

	@Override
	protected String getRScriptCode()
	{
		return "args <- commandArgs(TRUE)\n" //
				+ "\n" + RScriptUtil.installAndLoadPackage("tsne")
				+ "\n"
				+ "df = read.table(args[1])\n"
				+ "set.seed("
				+ randomSeed.getValue()
				+ ")\n" //
				+ "res <- tsne(df, k = 3, perplexity=" + getPerplexity() + ", max_iter="
				+ maxNumIterations.getValue()
				+ ", initial_dims=" + getInitialDims() + ")\n" + "print(head(res$ydata))\n"
				+ "\n"
				+ "##res <- smacofSphere.dual(df, ndim = 3)\n" + "#print(res$conf)\n"
				+ "#print(class(res$conf))\n"
				+ "\n" + "write.table(res$ydata,args[2]) \n" + "";
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
}
