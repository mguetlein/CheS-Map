package alg.embed3d.r;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Messages;
import gui.property.IntegerProperty;
import gui.property.Property;
import main.Settings;
import rscript.RScriptUtil;
import util.MessageUtil;
import util.StringLineAdder;
import alg.DistanceMeasure;
import alg.cluster.DatasetClusterer;
import alg.embed3d.AbstractRTo3DEmbedder;
import data.DatasetFile;

public class SMACOF3DEmbedder extends AbstractRTo3DEmbedder
{
	public static final SMACOF3DEmbedder INSTANCE = new SMACOF3DEmbedder();

	private SMACOF3DEmbedder()
	{
	}

	public int getMinNumInstances()
	{
		return 4; // else "Maximum number of dimensions is n-1!"
	}

	@Override
	protected String getShortName()
	{
		return "smacof";
	}

	@Override
	public String getName()
	{
		return Settings.text("embed.r.smacof");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("embed.r.smacof.desc", Settings.R_STRING);
	}

	IntegerProperty maxNumIterations = new IntegerProperty("Maximum number of iterations (itmax)", 150);

	@Override
	public Property[] getProperties()
	{
		return new Property[] { maxNumIterations };
	}

	@Override
	protected String getRScriptCode()
	{
		StringLineAdder s = new StringLineAdder();
		s.add("args <- commandArgs(TRUE)");
		s.add(RScriptUtil.installAndLoadPackage("smacof"));
		s.add("df = read.table(args[1])");
		s.add("d <- dist(df, method = \"euclidean\")");
		s.add("res <- smacofSym(d, ndim = 3, metric = FALSE, ties = \"secondary\", verbose = TRUE, itmax = "
				+ maxNumIterations.getValue() + ")");
		s.add("#res <- smacofSphere.dual(df, ndim = 3)");
		s.add("print(head(res$conf))");
		s.add("print(class(res$conf))");
		s.add("write.table(res$conf,args[2])");
		s.add("write.table(as.matrix(d),args[3])");
		return s.toString();
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		if (dataset.numCompounds() >= 50)
			m.add(MessageUtil.slowMessage(Settings.text("embed.r.smacof.slow", Sammon3DEmbedder.getNameStatic())));
		return m;
	}

	@Override
	public int getMinNumFeatures()
	{
		return 1;
	}

	@Override
	public boolean isLinear()
	{
		return false;
	}

	@Override
	public boolean isLocalMapping()
	{
		return false;
	}

	@Override
	protected String getErrorDescription(String errorOut)
	{
		return null;
	}

	@Override
	public DistanceMeasure getDistanceMeasure()
	{
		return DistanceMeasure.EUCLIDEAN_DISTANCE;
	}
}
