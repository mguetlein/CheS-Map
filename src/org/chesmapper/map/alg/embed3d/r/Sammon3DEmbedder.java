package org.chesmapper.map.alg.embed3d.r;

import org.chesmapper.map.alg.DistanceMeasure;
import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.alg.embed3d.AbstractRTo3DEmbedder;
import org.chesmapper.map.alg.r.DistanceProperty;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.gui.FeatureWizardPanel.FeatureInfo;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.rscript.RScriptUtil;
import org.mg.javalib.gui.Messages;
import org.mg.javalib.gui.property.DoubleProperty;
import org.mg.javalib.gui.property.IntegerProperty;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.util.StringLineAdder;

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
		return Settings.text("embed.r.sammon.desc", Settings.R_STRING) + "\n\n" + Settings.text("distance.desc");
	}

	IntegerProperty niter = new IntegerProperty("Maximum number of iterations (niter)", 100);
	DoubleProperty magic = new DoubleProperty(
			"Initial value of the step size constant in diagonal Newton method (magic)", 0.2);
	DoubleProperty tol = new DoubleProperty("Tolerance for stopping, in units of stress (tol)", 0.0001, 0.0, 1.0,
			0.00001);
	DistanceProperty dist_sim = new DistanceProperty(getName());
	IntegerProperty randomSeed = new IntegerProperty("Random seed", "Sammon random seed", 1);

	@Override
	public Property getRandomSeedProperty()
	{
		return randomSeed;
	}

	@Override
	public Property[] getProperties()
	{
		return new Property[] { niter, magic, tol, dist_sim, randomSeed };
	}

	public void enableTanimoto()
	{
		dist_sim.setValue("Tanimoto (similarity)");
	}

	@Override
	protected String getRScriptCode()
	{
		//		String dist_sim = this.dist_sim.getValue().toString();
		//		String dist_sim_method = null;
		String dist_sim_str = null;
		if (dist_sim.isDistanceSelected())
			dist_sim_str = "dist_method=\"" + dist_sim.getSelectedDistance() + "\"";
		else
			dist_sim_str = "sim_method=\"" + dist_sim.getSelectedSimilarity() + "\"";

		StringLineAdder s = new StringLineAdder();
		s.add("args <- commandArgs(TRUE)");
		s.add(RScriptUtil.installAndLoadPackage("MASS"));
		s.add(dist_sim.loadPackage());
		s.add(rCode);
		s.add("df = read.table(args[1])");
		s.add("set.seed(" + randomSeed.getValue() + ")");
		//s.add("save.image(\"/tmp/image.R\")");
		s.add("res <- sammon_duplicates(df, k=3, niter=" + niter.getValue() + ", magic=" + magic.getValue() + ", tol="
				+ tol.getValue() + ", " + dist_sim_str + " )");

		s.add("print(head(res))");
		s.add("write.table(res,args[2])");
		s.add("write.table(as.matrix(" + dist_sim.computeDistance("df") + "),args[3])");
		return s.toString();
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		dist_sim.addWarning(m, featureInfo);
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

	static String rCode = "duplicate_indices <- function( data ) {\n" //
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
			+ "}\n" //
			+ "sammon_duplicates <- function( data, dist_method=NULL, sim_method=NULL, k=3, ... ) {\n"// 
			+ "  di <- duplicate_indices(data)\n" + "  u <- unique(data)\n"// 
			+ "  print(paste('unique data points',nrow(u),'of',nrow(data)))\n"//
			+ "  if(nrow(u) <= 4) stop(\"number of unqiue datapoints <= 4\")\n"//
			+ "  if (!is.null(dist_method)) {\n" //
			+ "     print(paste(\"distance used: \",dist_method))\n"//
			+ "     distance = dist(u, method=dist_method)\n"//
			+ "  }\n"//
			+ "  else if (!is.null(sim_method)) {\n" //
			+ "     print(paste(\"similarity used: \",sim_method))\n"//
			+ "     distance = pr_simil2dist(simil(u, method=sim_method))\n"//
			+ "  }\n"//
			+ "  else stop(\"neither sim_method nor dist_method given\")\n" //
			+ "  scale <- cmdscale(distance, k)\n"//
			+ "  if (any(duplicated(scale))) {\n"//
			+ "  	print(\"jittering the initial configuration\")\n"//
			+ "  	scale <- jitter(scale)\n"//
			+ "  }\n"//
			+ "  points_unique <- sammon(distance, y=scale, k, ...)$points\n"// 
			+ "  points <- add_duplicates(points_unique, di)\n"// 
			+ "  points\n"// 
			+ "}\n"// 
			+ "";

	@Override
	protected String getErrorDescription(String errorOut)
	{
		if (errorOut.contains("only 2 of the first 3 eigenvalues are > 0"))
			return TOO_FEW_UNIQUE_DATA_POINTS;
		if (errorOut.contains("number of unqiue datapoints <= 4"))
			return TOO_FEW_UNIQUE_DATA_POINTS;
		else
			return null;
	}

	@Override
	public DistanceMeasure getDistanceMeasure()
	{
		return dist_sim.getDistanceMeasure();
	}
}
