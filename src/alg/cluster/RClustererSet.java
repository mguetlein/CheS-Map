package alg.cluster;

import gui.property.IntegerProperty;
import gui.property.Property;
import gui.property.SelectProperty;

import java.util.HashMap;

import main.Settings;
import rscript.RScriptUtil;

public class RClustererSet
{
	public static final DatasetClusterer[] R_CLUSTERER = new AbstractRClusterer[] { new KMeansRClusterer(),
			//new ModelBasedRClusterer(), 
			new CascadeKMeansRClusterer() };

	static class CascadeKMeansRClusterer extends AbstractRClusterer
	{

		@Override
		public String getName()
		{
			return "Cascade Kmeans (R)";
		}

		@Override
		public String getDescription()
		{
			return "Uses " + Settings.R_STRING + ".\n\n" + "bla.\n\n" + "http:// .html";
		}

		@Override
		protected String getRScriptName()
		{
			return "cascadeKM_" + minK + "_" + maxK + "_" + restart;
		}

		@Override
		protected String getRScriptCode()
		{
			String s = "args <- commandArgs(TRUE)\n";
			s += RScriptUtil.installAndLoadPackage("vegan");
			s += "df = read.table(args[1])\n";
			s += "if(" + maxK + " < " + minK + ") stop(\"min > max\")\n";
			s += "maxK <- min(" + maxK + ",nrow(unique(df)))\n";
			s += "if(maxK < " + minK + ") stop(\"min > num unique data points\")\n";
			s += "print(maxK)\n";
			s += "ccas <- cascadeKM(df, " + minK + ", maxK, iter = " + restart + ", criterion = \""
					+ critMap.get(criterion) + "\")\n";
			s += "max <- max.col(ccas$results)[2]\n";
			s += "print(ccas$results)\n";
			s += "print(ccas$partition[,max])\n";
			s += "write.table(ccas$partition[,max],args[2])\n";
			return s;
		}

		private final int minKDefault = 2;
		private int minK = minKDefault;
		public static final String PROPERTY_MIN_K = "minimum number of clusters (inf.gr)";

		private final int maxKDefault = 15;
		private int maxK = maxKDefault;
		public static final String PROPERTY_MAX_K = "maximum number of clusters (sup.gr)";

		private final int restartDefault = 100;
		private int restart = restartDefault;
		public static final String PROPERTY_RESTART = "number of restarts (iter)";

		private static final String calinski = "Calinski-Harabasz (1974) criterion (calinski)";
		private static final String ssi = "simple structure index (ssi)";
		public static final HashMap<String, String> critMap = new HashMap<String, String>();
		static
		{
			critMap.put(calinski, "calinski");
			critMap.put(ssi, "ssi");
		}
		private final String criterionDefault = calinski;
		private String criterion = criterionDefault;
		public static final String PROPERTY_CRITERION = "criterion to select the best partition (criterion)";

		private Property[] properties = new Property[] { new IntegerProperty(PROPERTY_MIN_K, minK, minKDefault),
				new IntegerProperty(PROPERTY_MAX_K, maxK, maxKDefault),
				new IntegerProperty(PROPERTY_RESTART, restart, restartDefault),
				new SelectProperty(PROPERTY_CRITERION, new String[] { calinski, ssi }, criterion, criterionDefault) };

		@Override
		public Property[] getProperties()
		{
			return properties;
		}

		@Override
		public void setProperties(Property[] properties)
		{
			for (Property property : properties)
			{
				if (property.getName().equals(PROPERTY_MIN_K))
					minK = ((IntegerProperty) property).getValue();
				if (property.getName().equals(PROPERTY_MAX_K))
					maxK = ((IntegerProperty) property).getValue();
				if (property.getName().equals(PROPERTY_RESTART))
					restart = ((IntegerProperty) property).getValue();
				if (property.getName().equals(PROPERTY_CRITERION))
					criterion = property.getValue().toString();
			}
		}
	}

	static class KMeansRClusterer extends AbstractRClusterer
	{

		@Override
		public String getName()
		{
			return "Kmeans (R)";
		}

		@Override
		public String getDescription()
		{
			return "Uses " + Settings.R_STRING + ".\n\n" + "kMeans clustering including random-restarts.\n\n"
					+ "http://stat.ethz.ch/R-manual/R-patched/library/stats/html/kmeans.html";
		}

		@Override
		protected String getRScriptName()
		{
			return "kmeans_" + k + "_" + restart;
		}

		@Override
		protected String getRScriptCode()
		{
			return "args <- commandArgs(TRUE)\n" //
					+ "\n" + "df = read.table(args[1])\n" + "res <- kmeans(df, " + k + ",nstart=" + restart
					+ ")\n"
					+ "print(res$cluster)\n" + "\n" + "print(res$withinss)\n"
					+ "\n"
					+ "write.table(res$cluster,args[2])\n";
		}

		private final int kDefault = 5;
		private int k = kDefault;
		public static final String PROPERTY_K = "number of clusters (k)";

		private final int restartDefault = 10;
		private int restart = restartDefault;
		public static final String PROPERTY_RESTART = "number of restarts (nstart)";

		private Property[] properties = new Property[] { new IntegerProperty(PROPERTY_K, k, kDefault),
				new IntegerProperty(PROPERTY_RESTART, restart, restartDefault) };

		@Override
		public Property[] getProperties()
		{
			return properties;
		}

		@Override
		public void setProperties(Property[] properties)
		{
			for (Property property : properties)
			{
				if (property.getName().equals(PROPERTY_K))
					k = ((IntegerProperty) property).getValue();
				if (property.getName().equals(PROPERTY_RESTART))
					restart = ((IntegerProperty) property).getValue();
			}
		}

		@Override
		public String getFixedNumClustersProperty()
		{
			return PROPERTY_K;
		}
	}

	static class ModelBasedRClusterer extends AbstractRClusterer
	{

		@Override
		public String getName()
		{
			return "Model Based (R)";
		}

		@Override
		public String getDescription()
		{
			return "Uses " + Settings.R_STRING + ".\n\n" + "bla.\n\n" + "http://.html";
		}

		@Override
		protected String getRScriptName()
		{
			return "mclust";
		}

		@Override
		protected String getRScriptCode()
		{
			return "args <- commandArgs(TRUE)\n" //
					+ "\n" + RScriptUtil.installAndLoadPackage("mclust") + "\n"
					+ "df = read.table(args[1])\n"
					+ "res <- Mclust(df)\n" + "print(res$classification)\n" + "\n" + "print(res$loglik)\n"
					+ "\n"
					+ "write.table(res$classification,args[2])\n";
		}
	}

}
