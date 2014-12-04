package org.chesmapper.map.alg.cluster;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.chesmapper.map.alg.DistanceMeasure;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.gui.FeatureWizardPanel.FeatureInfo;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.chesmapper.map.util.MessageUtil;
import org.chesmapper.map.weka.CascadeSimpleKMeans;
import org.chesmapper.map.weka.CompoundArffWriter;
import org.mg.javalib.gui.Messages;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.gui.property.PropertyUtil;
import org.mg.javalib.weka.WekaPropertyUtil;

import weka.clusterers.ClusterEvaluation;
import weka.clusterers.Clusterer;
import weka.clusterers.Cobweb;
import weka.clusterers.EM;
import weka.clusterers.FarthestFirst;
import weka.clusterers.HierarchicalClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;

public class WekaClusterer extends AbstractDatasetClusterer
{
	private static SimpleKMeans kMeans = new SimpleKMeans();
	static
	{
		try
		{
			kMeans.setNumClusters(8);
		}
		catch (Exception e)
		{
			Settings.LOGGER.error(e);
		}
	}

	private static String SKIP_PROPERTIES[] = new String[] { "saveInstanceData", "displayStdDevs", "debug",
			"displayModelInOldFormat", "printNewick" };
	private static final Clusterer[] CLUSTERER = new Clusterer[] { kMeans, new CascadeSimpleKMeans(),
			new FarthestFirst(), new EM(), new Cobweb(), new HierarchicalClusterer()
	//, new XMeans() 
	};

	public static WekaClusterer[] WEKA_CLUSTERER;
	static
	{
		WEKA_CLUSTERER = new WekaClusterer[CLUSTERER.length];
		int i = 0;
		for (Clusterer c : CLUSTERER)
		{
			WekaClusterer wc = new WekaClusterer(c);
			WEKA_CLUSTERER[i++] = wc;
		}
	}

	public static WekaClusterer getNewInstance(Clusterer wekaClusterer, Property[] properties)
	{
		return new WekaClusterer(wekaClusterer, properties);
	}

	Clusterer wekaClusterer;
	ClusterEvaluation eval;
	String additionalDescription;
	Property[] properties;
	String name;

	private WekaClusterer(Clusterer wekaClusterer)
	{
		this(wekaClusterer, null);
	}

	private WekaClusterer(Clusterer wekaClusterer, Property[] properties)
	{
		this.wekaClusterer = wekaClusterer;
		if (properties != null)
			this.properties = properties;
		else
			this.properties = WekaPropertyUtil.getProperties(wekaClusterer, SKIP_PROPERTIES);

		if (wekaClusterer instanceof Cobweb)
		{
			additionalDescription = Settings.text("cluster.weka.cobweb.desc");
			clusterApproach = ClusterApproach.Connectivity;
		}
		else if (wekaClusterer instanceof EM)
		{
			name = Settings.text("cluster.weka.em");
			additionalDescription = Settings.text("cluster.weka.em.desc");
			for (Property p : getProperties())
				if (p.getName().equals("numClusters"))
					p.setDisplayName("numClusters (-1 to automatically detect number of clusters)");
			clusterApproach = ClusterApproach.Distribution;
		}
		else if (wekaClusterer instanceof HierarchicalClusterer)
		{
			name = Settings.text("cluster.weka.hierarchical");
			additionalDescription = Settings.text("cluster.weka.hierarchical.desc");
			clusterApproach = ClusterApproach.Connectivity;
		}
		else if (wekaClusterer instanceof SimpleKMeans)
		{
			additionalDescription = Settings.text("cluster.weka.kmeans.desc", Settings.text("cluster.weka.cascade"));
			clusterApproach = ClusterApproach.Centroid;
		}
		else if (wekaClusterer instanceof FarthestFirst)
		{
			additionalDescription = Settings.text("cluster.weka.farthest.desc", Settings.text("cluster.weka.cascade"));
			clusterApproach = ClusterApproach.Centroid;
		}
		else if (wekaClusterer instanceof CascadeSimpleKMeans)
		{
			name = Settings.text("cluster.weka.cascade");
			additionalDescription = Settings.text("cluster.weka.cascade.desc");
			clusterApproach = ClusterApproach.Centroid;
		}
		else
			throw new IllegalStateException("unknown cluster approach");
	}

	@Override
	public Property getFixedNumClustersProperty()
	{
		return PropertyUtil.getProperty(properties, "numClusters");
	}

	@Override
	public Property getRandomSeedProperty()
	{
		return PropertyUtil.getProperty(properties, "seed");
	}

	@Override
	public Property getRandomRestartProperty()
	{
		return PropertyUtil.getProperty(properties, "restarts");
	}

	@Override
	public Property getDistanceFunctionProperty()
	{
		return PropertyUtil.getProperty(properties, "distanceFunction");
	}

	@Override
	protected List<Integer[]> cluster(DatasetFile dataset, List<CompoundData> compounds, List<CompoundProperty> features)
	{
		WekaPropertyUtil.setProperties(wekaClusterer, properties);

		TaskProvider.debug("Converting data to arff-format");
		File f = CompoundArffWriter.writeArffFile(dataset, compounds, features);

		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(f));
			Instances data = new Instances(reader);

			eval = new ClusterEvaluation();
			TaskProvider.debug("Building clusterer");
			wekaClusterer.buildClusterer(data);
			eval.setClusterer(wekaClusterer);
			TaskProvider.debug("Clustering dataset");
			eval.evaluateClusterer(data);
			Settings.LOGGER.info("# of clusters: " + eval.getNumClusters());

			List<Integer[]> clusterAssignements = new ArrayList<Integer[]>();
			for (int j = 0; j < eval.getClusterAssignments().length; j++)
				clusterAssignements.add(new Integer[] { (int) eval.getClusterAssignments()[j] });
			return clusterAssignements;
		}
		catch (Exception e)
		{
			// error is caught in mapping process, message displayed in dialog and stack trace (with error cause) is printed
			throw new Error("Error occured while clustering with WEKA: " + e.getMessage(), e);
		}
	}

	@Override
	public String getShortName()
	{
		return wekaClusterer.getClass().getSimpleName();
	}

	@Override
	public Property[] getProperties()
	{
		return properties;
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		if (wekaClusterer instanceof EM || wekaClusterer instanceof CascadeSimpleKMeans)
		{
			if (featureInfo.isNumPairsHigh())
				m.add(MessageUtil.slowRuntimeMessage(featureInfo.getNumPairsWarning()));
		}
		return m;
	}

	@Override
	public String getName()
	{
		if (name == null)
			return wekaClusterer.getClass().getSimpleName() + " (WEKA)";
		else
			return name + " (WEKA)";
	}

	@Override
	public String getDescription()
	{
		String s = "Uses " + Settings.WEKA_STRING + ".\n";
		if (additionalDescription != null)
			s += additionalDescription + "\n";
		s += "\n";
		if (!(wekaClusterer instanceof CascadeSimpleKMeans))
			s += "<i>WEKA API documentation:</i> http://weka.sourceforge.net/doc.stable/weka/clusterers/"
					+ wekaClusterer.getClass().getSimpleName() + ".html\n\n";

		// weka has no interface for globalInfo
		try
		{
			Method m[] = wekaClusterer.getClass().getDeclaredMethods();
			for (Method method : m)
			{
				if (method.getName().equals("globalInfo"))
				{
					s += "<i>Internal WEKA description:</i>\n";
					s += method.invoke(wekaClusterer, (Object[]) null).toString().replaceAll("\n\n", "\n");
					break;
				}
			}
		}
		catch (Exception e)
		{
			Settings.LOGGER.error(e);
		}
		return s;
	}

	public Clusterer getWekaClusterer()
	{
		return wekaClusterer;
	}

	@Override
	public DistanceMeasure getDistanceMeasure()
	{
		return DistanceMeasure.EUCLIDEAN_DISTANCE;
	}

}
