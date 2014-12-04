package org.chesmapper.map.alg;

import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.alg.cluster.NoClusterer;
import org.chesmapper.map.alg.cluster.WekaClusterer;
import org.chesmapper.map.alg.embed3d.Random3DEmbedder;
import org.chesmapper.map.alg.embed3d.ThreeDEmbedder;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.Settings;
import org.mg.javalib.datamining.ResultSet;

import weka.clusterers.EM;

public class AlgorithmUtil
{
	public static void printProperties(Algorithm algs[])
	{
		ResultSet s = new ResultSet();
		for (Algorithm a : algs)
		{
			if (a instanceof NoClusterer)
				continue;
			if (a instanceof Random3DEmbedder)
				continue;

			int r = s.addResult();
			s.setResultValue(r, "Algorithm", a.getName());
			if (a instanceof DatasetClusterer)
			{
				DatasetClusterer c = (DatasetClusterer) a;
				s.setResultValue(r, "Cluster approach", c.getClusterApproach());

				//s.setResultValue(r, "Number of clusters fixed", c.getFixedNumClustersProperty() == null ? "No" : "Yes");
				if (c instanceof WekaClusterer && ((WekaClusterer) c).getWekaClusterer() instanceof EM)
					s.setResultValue(r, "Num Clusters Variable", "Yes*");
				else
					s.setResultValue(r, "Num Clusters Variable", c.getFixedNumClustersProperty() == null ? "Yes" : "");

				//				s.setResultValue(r, "Distance Function", c.getDistanceFunctionProperty() == null ? "Euclidean"
				//						: "Various");
				s.setResultValue(r, "Various Distance Functions", c.getDistanceFunctionProperty() == null ? "" : "Yes");
			}
			else if (a instanceof ThreeDEmbedder)
			{
				ThreeDEmbedder t = (ThreeDEmbedder) a;
				s.setResultValue(r, "Linear", t.isLinear() ? "Yes" : "");
				s.setResultValue(r, "Local", t.isLocalMapping() ? "Yes" : "");
			}

			s.setResultValue(r, "Deterministic",
					(a.getRandomSeedProperty() == null && a.getRandomRestartProperty() == null) ? "Yes" : "");

			if (!(a instanceof ThreeDEmbedder))
				s.setResultValue(r, "Random restarts", a.getRandomRestartProperty() != null ? "Yes" : "");

			s.setResultValue(r, "Runs without R", a.getBinary() == BinHandler.RSCRIPT_BINARY ? "" : "Yes");
		}
		Settings.LOGGER.info(s.toNiceString());
		Settings.LOGGER.info();
		Settings.LOGGER.info(s.toMediaWikiString());
	}

	public static void main(String args[])
	{
		//		printProperties(ClusterWizardPanel.CLUSTERERS);
		printProperties(ThreeDEmbedder.EMBEDDERS);
	}
}
