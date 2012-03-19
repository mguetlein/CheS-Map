package alg.embed3d.r;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Messages;
import gui.property.DoubleProperty;
import gui.property.IntegerProperty;
import gui.property.Property;
import main.Settings;
import rscript.RScriptUtil;
import alg.cluster.DatasetClusterer;
import alg.embed3d.AbstractRTo3DEmbedder;
import data.DatasetFile;

public class Sammon3DEmbedder extends AbstractRTo3DEmbedder
{
	public static final Sammon3DEmbedder INSTANCE = new Sammon3DEmbedder();

	private Sammon3DEmbedder()
	{
	}

	public int getMinNumInstances()
	{
		return 4;
	}

	@Override
	protected String getShortName()
	{
		return "sammon";
	}

	@Override
	public String getName()
	{
		return getNameStatic();
	}

	public static String getNameStatic()
	{
		return Settings.text("embed.r.sammon");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("embed.r.sammon.desc", Settings.R_STRING);
	}

	IntegerProperty niter = new IntegerProperty("Maximum number of iterations (niter)", 100);
	DoubleProperty magic = new DoubleProperty(
			"Initial value of the step size constant in diagonal Newton method (magic)", 0.2);
	DoubleProperty tol = new DoubleProperty("Tolerance for stopping, in units of stress (tol)", 0.0001, 0.0, 1.0,
			0.00001);

	@Override
	public Property[] getProperties()
	{
		return new Property[] { niter, magic, tol };
	}

	@Override
	protected String getRScriptCode()
	{
		return "args <- commandArgs(TRUE)\n" + "\n"
				+ // 
				RScriptUtil.installAndLoadPackage("MASS") + "\n"
				+ //
				rCode + "\n"
				+ "df = read.table(args[1])\n" //
				+ "res <- sammon_duplicates(df, k=3, niter=" + niter.getValue() + ", magic=" + magic.getValue()
				+ ", tol=" + tol.getValue() + " )\n" + //
				"print(head(res))\n" + //
				"write.table(res,args[2]) ";
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		//			if (dataset.numCompounds() >= 50)
		//				m.add(Message.slowMessage(Settings.text("embed.r.smacof.slow", PCAFeature3DEmbedder.getNameStatic())));
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

	String rCode = "duplicate_indices <- function( data ) {\n" //
			+ "  indices = 1:nrow(data)\n"// 
			+ "  z = data\n"
			+ "  duplicate_index = anyDuplicated(z)\n"// 
			+ "  while(duplicate_index) {\n"
			+ "    duplicate_to_index = anyDuplicated(z[1:duplicate_index,],fromLast=T)\n"
			+ "    #print(paste(duplicate_index,'is dupl to',duplicate_to_index))\n"
			+ "    indices[duplicate_index] <- duplicate_to_index\n"
			+ "    z[duplicate_index,] <- paste('123$§%',duplicate_index)\n"
			+ "    duplicate_index = anyDuplicated(z)\n"// 
			+ "  }\n"// 
			+ "  indices\n"// 
			+ "}\n"
			+ "add_duplicates <- function( data, dup_indices ) {\n"// 
			+ "  result = data[1,]\n"
			+ "  for(i in 2:length(dup_indices)) {\n"// 
			+ "    row = data[rownames(data)==dup_indices[i],]\n"
			+ "    if(length(row)==0)\n"
			+ "       stop(paste('index ',i,' dup-index ',dup_indices[i],'not found in data'))\n"
			+ "    result = rbind(result, row)\n"// 
			+ "  }\n"// 
			+ "  rownames(result)<-NULL\n"// 
			+ "  result\n"// 
			+ "}\n"
			+ "sammon_duplicates <- function( data, ... ) {\n"// 
			+ "  di <- duplicate_indices(data)\n"
			+ "  u <- unique(data)\n"// 
			+ "  print(paste('unique data points',nrow(u),'of',nrow(data)))\n"
			+ "  if(nrow(u) <= 4) stop(\""
			+ TOO_FEW_UNIQUE_DATA_POINTS + "\")\n"
			+ "  points_unique <- sammon(dist(u), ...)$points\n"
			+ "  points <- add_duplicates(points_unique, di)\n"// 
			+ "  points\n"// 
			+ "}\n"// 
			+ "";
}
