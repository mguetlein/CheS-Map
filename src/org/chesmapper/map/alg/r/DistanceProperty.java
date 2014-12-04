package org.chesmapper.map.alg.r;

import java.util.LinkedHashMap;

import org.chesmapper.map.alg.DistanceMeasure;
import org.chesmapper.map.gui.FeatureWizardPanel.FeatureInfo;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.rscript.RScriptUtil;
import org.mg.javalib.gui.Message;
import org.mg.javalib.gui.Messages;
import org.mg.javalib.gui.property.SelectProperty;
import org.mg.javalib.util.ArrayUtil;

public class DistanceProperty extends SelectProperty
{
	private static LinkedHashMap<Object, String> d = new LinkedHashMap<Object, String>();
	static
	{
		d.put("Braun-Blanquet (similarity)", "type: binary, formula: a / max{(a + b), (a + c)})");
		d.put("Chi-squared (similarity)", "type: nominal, formula: sum_ij (o_i - e_i)^2 / e_i)");
		d.put("correlation (similarity)", "type: metric, formula: xy / sqrt(xx * yy) for centered x,y)");
		d.put("cosine (similarity)", "type: metric, formula: xy / sqrt(xx * yy))");
		d.put("Cramer (similarity)", "type: nominal, formula: sqrt{[Chi / n)] / min[(p - 1), (q - 1)]})");
		d.put("Dice (similarity)", "type: binary, formula: 2a / (2a + b + c))");
		d.put("eJaccard (similarity)", "type: metric, formula: xy / (xx + yy - xy))");
		d.put("Fager (similarity)", "type: binary, formula: a / sqrt((a + b)(a + c)) - sqrt(a + c) / 2)");
		d.put("Faith (similarity)", "type: binary, formula: (a + d/2) / n)");
		d.put("fJaccard (similarity)", "type: metric, formula: sum_i (min{x_i, y_i} / max{x_i, y_i}))");
		//		d.put("Gower (similarity)", "type: NA, formula: Sum_k (s_ijk * w_k) / Sum_k (d_ijk * w_k))");
		d.put("Hamman (similarity)", "type: binary, formula: ([a + d] - [b + c]) / n)");
		d.put("Jaccard (similarity)", "type: binary, formula: a / (a + b + c))");
		d.put("Kulczynski1 (similarity)", "type: binary, formula: a / (b + c))");
		d.put("Kulczynski2 (similarity)", "type: binary, formula: [a / (a + b) + a / (a + c)] / 2)");
		d.put("Michael (similarity)", "type: binary, formula: 4(ad - bc) / [(a + d)^2 + (b + c)^2])");
		d.put("Mountford (similarity)", "type: binary, formula: 2a / (ab + ac + 2bc))");
		d.put("Mozley (similarity)", "type: binary, formula: an / (a + b)(a + c))");
		d.put("Ochiai (similarity)", "type: binary, formula: a / sqrt[(a + b)(a + c)])");
		d.put("Pearson (similarity)", "type: nominal, formula: sqrt{Chi / (n + Chi)})");
		d.put("Phi (similarity)", "type: binary, formula: (ad - bc) / sqrt[(a + b)(c + d)(a + c)(b + d)])");
		d.put("Phi-squared (similarity)", "type: nominal, formula: [sum_ij (o_i - e_i)^2 / e_i] / n)");
		d.put("Russel (similarity)", "type: binary, formula: a / n)");
		d.put("simple matching (similarity)", "type: binary, formula: (a + d) / n)");
		d.put("Simpson (similarity)", "type: binary, formula: a / min{(a + b), (a + c)})");
		d.put("Stiles (similarity)",
				"type: binary, formula: log(n(|ad-bc| - 0.5n)^2 / [(a + b)(c + d)(a + c)(b + d)]))");
		d.put("Tanimoto (similarity)", "type: binary, formula: (a + d) / (a + 2b + 2c + d))");
		d.put("Tschuprow (similarity)",
				"type: nominal, formula: sqrt{[sum_ij (o_i - e_i)^2 / e_i] / n / sqrt((p - 1)(q - 1))})");
		d.put("Yule (similarity)", "type: binary, formula: (ad - bc) / (ad + bc))");
		d.put("Yule2 (similarity)", "type: binary, formula: (sqrt(ad) - sqrt(bc)) / (sqrt(ad) + sqrt(bc)))");
		d.put("Bhjattacharyya (distance)", "type: metric, formula: sqrt(sum_i (sqrt(x_i) - sqrt(y_i))^2)))");
		d.put("Bray (distance)", "type: metric, formula: sum_i |x_i - y_i| / sum_i (x_i + y_i))");
		d.put("Canberra (distance)", "type: metric, formula: sum_i |x_i - y_i| / |x_i + y_i|)");
		d.put("Chord (distance)", "type: metric, formula: sqrt(2 * (1 - xy / sqrt(xx * yy))))");
		d.put("divergence (distance)", "type: metric, formula: sum_i (x_i - y_i)^2 / (x_i + y_i)^2)");
		d.put("Euclidean (distance)", "type: metric, formula: sqrt(sum_i (x_i - y_i)^2)))");
		d.put("Geodesic (distance)", "type: metric, formula: arccos(xy / sqrt(xx * yy)))");
		d.put("Hellinger (distance)",
				"type: metric, formula: sqrt(sum_i (sqrt(x_i / sum_i x) - sqrt(y_i / sum_i y)) ^ 2))");
		d.put("Kullback (distance)",
				"type: metric, formula: sum_i [x_i * log((x_i / sum_j x_j) / (y_i / sum_j y_j)) / sum_j x_j)])");
		//		d.put("Levenshtein (distance)",
		//				"type: other, formula: Number of insertions, edits, and deletions between to strings)");
		d.put("Mahalanobis (distance)", "type: metric, formula: sqrt((x - y) Sigma^(-1) (x - y)))");
		d.put("Manhattan (distance)", "type: metric, formula: sum_i |x_i - y_i|)");
		d.put("Minkowski (distance)", "type: metric, formula: (sum_i (x_i - y_i)^p)^(1/p))");
		d.put("Podani (distance)", "type: metric, formula: 1 - 2 * (a - b + c - d) / (n * (n - 1)))");
		d.put("Soergel (distance)", "type: metric, formula: sum_i |x_i - y_i| / sum_i max{x_i, y_i})");
		d.put("supremum (distance)", "type: metric, formula: max_i |x_i - y_i|)");
		d.put("Wave (distance)", "type: metric, formula: sum_i (1 - min(x_i, y_i) / max(x_i, y_i)))");
		d.put("Whittaker (distance)", "type: metric, formula: sum_i |x_i / sum_i x - y_i / sum_i y| / 2)");

		for (Object k : d.keySet())
		{
			String s = k.toString().replace("(", "").replace(")", "");
			d.put(k, "<html>" + s + "<br><span style=\"font-size:75%; color:#6E6E6E\">" + d.get(k) + "</span></html>");
		}
	}

	private static String DEFAULT_DISTANCE = "Euclidean (distance)";
	private static String NAME = "Distance / similarity measure";

	public DistanceProperty(String uniqSuffix)
	{
		super(NAME, NAME + uniqSuffix, ArrayUtil.toArray(d.keySet()), DEFAULT_DISTANCE);
		setRenderValues(d);
	}

	public DistanceMeasure getDistanceMeasure()
	{
		if (isSimilaritySelected())
			return new DistanceMeasure(getSelectedSimilarity());
		else
			return new DistanceMeasure(getSelectedDistance());
	}

	public void addWarning(Messages m, FeatureInfo featureInfo)
	{
		if (featureInfo.isFeaturesSelected())
		{
			if (isSelectedTypeBinary())
			{
				if (!featureInfo.isOnlyBinaryFeaturesSelected())
					m.add(Message.errorMessage(Settings.text("distance.err.bin-not-possible")));
			}
			else if (isSelectedTypeNominal())
			{
				if (!featureInfo.isOnlyNominalFeaturesSelected())
					m.add(Message.errorMessage(Settings.text("distance.err.nom-not-possible")));
			}
			else if (isSelectedTypeMetric())
			{
				if (featureInfo.isOnlyBinaryFeaturesSelected())
				{
					if (featureInfo.isFragmentFeatureSelected())
						m.add(Message.warningMessage(Settings.text("distance.err.prefer-binary-frag")));
					else
						m.add(Message.warningMessage(Settings.text("distance.err.prefer-binary")));
				}
			}
			else
				throw new IllegalStateException();
		}
	}

	private boolean isSelectedTypeBinary()
	{
		return d.get(getValue()).contains("type: binary");
	}

	private boolean isSelectedTypeNominal()
	{
		return d.get(getValue()).contains("type: nominal");
	}

	private boolean isSelectedTypeMetric()
	{
		return d.get(getValue()).contains("type: metric");
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
