package alg.cluster;

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
import alg.cluster.r.KMeansRClusterer;
import data.ClusterDataImpl;
import data.DatasetFile;
import dataInterface.ClusterData;
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
	private static final Clusterer[] CLUSTERER = new Clusterer[] { new EM(), new Cobweb(), new HierarchicalClusterer(),
			kMeans, new FarthestFirst(), };

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
				wc.name = Settings.text("cluster.weka.hierachical");
				wc.additionalDescription = Settings.text("cluster.weka.hierachical.desc");
			}
			else if (c instanceof SimpleKMeans)
			{
				wc.additionalDescription = Settings.text("cluster.weka.kmeans.desc", KMeansRClusterer.getNameStatic());
			}
			else if (c instanceof FarthestFirst)
			{
				wc.additionalDescription = Settings
						.text("cluster.weka.farthest.desc", KMeansRClusterer.getNameStatic());
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
		this.wekaClusterer = wekaClusterer;
		properties = WekaPropertyUtil.getProperties(wekaClusterer, SKIP_PROPERTIES);
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
	public void clusterDataset(DatasetFile dataset, List<CompoundData> compounds, List<MoleculeProperty> features)
	{
		WekaPropertyUtil.setProperties(wekaClusterer, properties);

		TaskProvider.task().verbose("Converting data to arff-format");
		File f = CompoundArffWriter.writeArffFile(ListUtil.cast(MolecularPropertyOwner.class, compounds), features);
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

			clusters = new ArrayList<ClusterData>();
			int numClusters = eval.getNumClusters();
			for (int i = 0; i < numClusters; i++)
			{
				ClusterDataImpl c = new ClusterDataImpl();
				clusters.add(c);
			}
			for (int j = 0; j < eval.getClusterAssignments().length; j++)
				((ClusterDataImpl) clusters.get((int) eval.getClusterAssignments()[j])).addCompound(compounds.get(j));

			List<Integer> toDelete = new ArrayList<Integer>();
			int i = 0;
			for (ClusterData c : clusters)
			{
				if (c.getSize() == 0)
					toDelete.add(i);
				i++;
			}
			for (int j = toDelete.size() - 1; j >= 0; j--)
				clusters.remove(toDelete.get(j).intValue());

			storeClusters(dataset.getSDFPath(true), wekaClusterer.getClass().getSimpleName(), getName(), clusters);
		}
		catch (Exception e)
		{
			// error is caught in mapping process, message displayed in dialog and stack trace (with error cause) is printed
			throw new Error("Error occured while clustering with WEKA: " + e.getMessage(), e);
		}
	}

	@Override
	public Property[] getProperties()
	{
		return properties;
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
		s += "<i>WEKA API documentation:</i> http://weka.sourceforge.net/doc.dev/weka/clusterers/"
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
