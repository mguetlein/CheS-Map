package alg.embed3d.r;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Messages;
import gui.property.DoubleProperty;
import gui.property.IntegerProperty;
import gui.property.Property;
import gui.property.SelectProperty;
import main.Settings;
import rscript.RScriptUtil;
import util.StringLineAdder;
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
	SelectProperty dist_sim = new SelectProperty("Distance / similarity measure",
			new String[] { "Braun-Blanquet (similarity)", "Chi-squared (similarity)", "correlation (similarity)",
					"cosine (similarity)", "Cramer (similarity)", "Dice (similarity)", "eJaccard (similarity)",
					"Fager (similarity)", "Faith (similarity)", "fJaccard (similarity)", "Gower (similarity)",
					"Hamman (similarity)", "Jaccard (similarity)", "Kulczynski1 (similarity)",
					"Kulczynski2 (similarity)", "Michael (similarity)", "Mountford (similarity)",
					"Mozley (similarity)", "Ochiai (similarity)", "Pearson (similarity)", "Phi (similarity)",
					"Phi-squared (similarity)", "Russel (similarity)", "simple matching (similarity)",
					"Simpson (similarity)", "Stiles (similarity)", "Tanimoto (similarity)", "Tschuprow (similarity)",
					"Yule (similarity)", "Yule2 (similarity)", "Bhjattacharyya (distance)", "Bray (distance)",
					"Canberra (distance)", "Chord (distance)", "divergence (distance)", "Euclidean (distance)",
					"Geodesic (distance)", "Hellinger (distance)", "Kullback (distance)", "Levenshtein (distance)",
					"Mahalanobis (distance)", "Manhattan (distance)", "Minkowski (distance)", "Podani (distance)",
					"Soergel (distance)", "supremum (distance)", "Wave (distance)", "Whittaker (distance)" },
			"Euclidean (distance)");

	@Override
	public Property[] getProperties()
	{
		return new Property[] { niter, magic, tol, dist_sim };
	}

	@Override
	protected String getRScriptCode()
	{
		String dist_sim = this.dist_sim.getValue().toString();
		String dist_sim_method = null;
		String dist_sim_str = null;
		if (dist_sim.endsWith("(distance)"))
		{
			dist_sim_method = dist_sim.replace(" (distance)", "");
			dist_sim_str = ",dist_method=\"" + dist_sim_method + "\"";
		}
		else if (dist_sim.endsWith("(similarity)"))
		{
			dist_sim_method = dist_sim.replace(" (similarity)", "");
			dist_sim_str = ",sim_method=\"" + dist_sim_method + "\"";
		}
		else
			throw new Error("WTF");

		StringLineAdder s = new StringLineAdder();
		s.add("args <- commandArgs(TRUE)");
		s.add(RScriptUtil.installAndLoadPackage("MASS"));
		s.add(RScriptUtil.installAndLoadPackage("proxy"));
		s.add(rCode);
		s.add("df = read.table(args[1])");
		//s.add("save.image(\"/tmp/image.R\")");
		s.add("res <- sammon_duplicates(df, k=3, niter=" + niter.getValue() + ", magic=" + magic.getValue() + ", tol="
				+ tol.getValue() + " " + dist_sim_str + " )");
		s.add("print(head(res))");
		s.add("write.table(res,args[2])");
		if (dist_sim.endsWith("(distance)"))
			s.add("write.table(as.matrix(dist(df, method=\"" + dist_sim_method + "\")),args[3])");
		else
			s.add("write.table(as.matrix(pr_simil2dist(simil(df, method=\"" + dist_sim_method + "\"))),args[3])");
		return s.toString();
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
			+ "sammon_duplicates <- function( data, dist_method=NULL, sim_method=NULL, ... ) {\n"// 
			+ "  di <- duplicate_indices(data)\n"
			+ "  u <- unique(data)\n"// 
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
			+ "  else stop(\"neither sim_method nor dist_method given\")\n"
			+ "  points_unique <- sammon(distance, ...)$points\n"// 
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
}
