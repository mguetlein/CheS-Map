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
		if (dataset.numCompounds() >= 50 && featureInfo.isNumFeaturesHigh())
			m.add(MessageUtil.slowMessage(featureInfo.getNumFeaturesWarning()));
		return m;
	}

	@Override
	protected String getDefaultError()
	{
		return "t-SNE tends to fail with improper input data (especially on small datasets). Try a different embedding algorithm.";
	}

	@Override
	protected String getRScriptCode()
	{
		String s = "args <- commandArgs(TRUE)\n";
		s += RScriptUtil.installAndLoadPackage("tsne") + "\n";
		s += "df = read.table(args[1])\n";
		s += "set.seed(" + randomSeed.getValue() + ")\n";
		s += "inst <- nrow(unique(df))\n";
		s += "print(paste('unique instances ',inst))\n";
		s += "if(inst < 3) stop(\"" + TOO_FEW_UNIQUE_DATA_POINTS + "\")\n";
		s += "perp <- min(" + perplexity.getValue() + ",inst)\n";
		s += "feats <- ncol(df)\n";
		s += "print(paste('features ',feats))\n";
		s += "dims <- min(" + initial_dims.getValue() + ",feats)\n";
		s += "res <- tryCatch(tsne(df, k = 3, perplexity=perp, max_iter=" + maxNumIterations.getValue()
				+ ", initial_dims=dims),error=function(e) stop(\"" + getDefaultError() + "\"))\n";
		s += "print(head(res$ydata))\n";
		//+ "##res <- smacofSphere.dual(df, ndim = 3)\n" + "#print(res$conf)\n" + "#print(class(res$conf))\n"
		s += "write.table(res$ydata,args[2]) \n";
		return s;
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
