package alg.r;

import gui.property.SelectProperty;
import rscript.RScriptUtil;

public class DistanceProperty extends SelectProperty
{
	private static String[] DISTANCES = new String[] { "Braun-Blanquet (similarity)", "Chi-squared (similarity)",
			"correlation (similarity)", "cosine (similarity)", "Cramer (similarity)", "Dice (similarity)",
			"eJaccard (similarity)", "Fager (similarity)", "Faith (similarity)", "fJaccard (similarity)",
			"Gower (similarity)", "Hamman (similarity)", "Jaccard (similarity)", "Kulczynski1 (similarity)",
			"Kulczynski2 (similarity)", "Michael (similarity)", "Mountford (similarity)", "Mozley (similarity)",
			"Ochiai (similarity)", "Pearson (similarity)", "Phi (similarity)", "Phi-squared (similarity)",
			"Russel (similarity)", "simple matching (similarity)", "Simpson (similarity)", "Stiles (similarity)",
			"Tanimoto (similarity)", "Tschuprow (similarity)", "Yule (similarity)", "Yule2 (similarity)",
			"Bhjattacharyya (distance)", "Bray (distance)", "Canberra (distance)", "Chord (distance)",
			"divergence (distance)", "Euclidean (distance)", "Geodesic (distance)", "Hellinger (distance)",
			"Kullback (distance)", "Levenshtein (distance)", "Mahalanobis (distance)", "Manhattan (distance)",
			"Minkowski (distance)", "Podani (distance)", "Soergel (distance)", "supremum (distance)",
			"Wave (distance)", "Whittaker (distance)" };
	private static String DEFAULT_DISTANCE = "Euclidean (distance)";
	private static String NAME = "Distance / similarity measure";

	public DistanceProperty(String uniqSuffix)
	{
		super(NAME, NAME + uniqSuffix, DISTANCES, DEFAULT_DISTANCE);
	}

	public boolean isDistanceSelected()
	{
		return getValue().toString().endsWith("(distance)");
	}

	public boolean isSimilaritySelected()
	{
		return getValue().toString().endsWith("(similarity)");
	}

	public String getSelectedDistance()
	{
		if (isDistanceSelected())
			return getValue().toString().replace(" (distance)", "");
		else
			return null;
	}

	public String getSelectedSimilarity()
	{
		if (isSimilaritySelected())
			return getValue().toString().replace(" (similarity)", "");
		else
			return null;
	}

	public String computeDistance(String dataframe)
	{
		if (isDistanceSelected())
			return "dist(" + dataframe + ", method=\"" + getSelectedDistance() + "\")";
		else
			return "pr_simil2dist(simil(" + dataframe + ", method=\"" + getSelectedSimilarity() + "\"))";
	}

	public String loadPackage()
	{
		return RScriptUtil.installAndLoadPackage("proxy");
	}

}
