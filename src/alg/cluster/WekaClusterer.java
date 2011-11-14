package alg.cluster;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Message;
import gui.Messages;
import gui.property.Property;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import main.Settings;
import main.TaskProvider;
import util.ListUtil;
import weka.CascadeSimpleKMeans;
import weka.CompoundArffWriter;
import weka.WekaPropertyUtil;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.Clusterer;
import weka.clusterers.Cobweb;
import weka.clusterers.EM;
import weka.clusterers.FarthestFirst;
import weka.clusterers.HierarchicalClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;
import data.DatasetFile;
import dataInterface.CompoundData;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

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
			e.printStackTrace();
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
			if (c instanceof Cobweb)
			{
				wc.additionalDescription = Settings.text("cluster.weka.cobweb.desc");
			}
			else if (c instanceof EM)
			{
				wc.name = Settings.text("cluster.weka.em");
				wc.additionalDescription = Settings.text("cluster.weka.em.desc");
				for (Property p : wc.getProperties())
					if (p.getName().equals("numClusters"))
						p.setDisplayName("numClusters (-1 to automatically detect number of clusters)");
			}
			else if (c instanceof HierarchicalClusterer)
			{
				wc.name = Settings.text("cluster.weka.hierarchical");
				wc.additionalDescription = Settings.text("cluster.weka.hierarchical.desc");
			}
			else if (c instanceof SimpleKMeans)
			{
				wc.additionalDescription = Settings.text("cluster.weka.kmeans.desc",
						Settings.text("cluster.weka.cascade"));
			}
			else if (c instanceof FarthestFirst)
			{
				wc.additionalDescription = Settings.text("cluster.weka.farthest.desc",
						Settings.text("cluster.weka.cascade"));
			}
			else if (c instanceof CascadeSimpleKMeans)
			{
				wc.name = Settings.text("cluster.weka.cascade");
				wc.additionalDescription = Settings.text("cluster.weka.cascade.desc");
			}
			WEKA_CLUSTERER[i++] = wc;
		}
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

	public WekaClusterer(Clusterer wekaClusterer, Property[] properties)
	{
		this.wekaClusterer = wekaClusterer;
		if (properties != null)
			this.properties = properties;
		else
			this.properties = WekaPropertyUtil.getProperties(wekaClusterer, SKIP_PROPERTIES);
	}

	@Override
	public String getFixedNumClustersProperty()
	{
		if (wekaClusterer instanceof SimpleKMeans || wekaClusterer instanceof FarthestFirst
				|| wekaClusterer instanceof HierarchicalClusterer)
			return "numClusters";
		else
			return null;
	}

	@Override
	protected List<Integer> cluster(DatasetFile dataset, List<CompoundData> compounds, List<MoleculeProperty> features)
	{
		WekaPropertyUtil.setProperties(wekaClusterer, properties);

		TaskProvider.task().verbose("Converting data to arff-format");
		File f = CompoundArffWriter.writeArffFile(dataset, ListUtil.cast(MolecularPropertyOwner.class, compounds),
				features);

		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(f));
			Instances data = new Instances(reader);

			eval = new ClusterEvaluation();
			TaskProvider.task().verbose("Building clusterer");
			wekaClusterer.buildClusterer(data);
			eval.setClusterer(wekaClusterer);
			TaskProvider.task().verbose("Clustering dataset");
			eval.evaluateClusterer(data);
			System.out.println("# of clusters: " + eval.getNumClusters());

			List<Integer> clusterAssignements = new ArrayList<Integer>();
			for (int j = 0; j < eval.getClusterAssignments().length; j++)
				clusterAssignements.add((int) eval.getClusterAssignments()[j]);
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
			if (dataset.numCompounds() >= 50 && featureInfo.isNumFeaturesHigh())
				m.add(Message.slowMessage(featureInfo.getNumFeaturesWarning()));
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
			e.printStackTrace();
		}
		return s;
	}

}
