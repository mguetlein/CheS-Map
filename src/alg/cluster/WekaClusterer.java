package alg.cluster;

import gui.property.Property;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
import weka.core.Version;
import data.ClusterDataImpl;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

public class WekaClusterer implements DatasetClusterer
{
	private static final Clusterer[] CLUSTERER = new Clusterer[] { new SimpleKMeans(), new Cobweb(), new EM(),
			new FarthestFirst(), new HierarchicalClusterer() };
	public static WekaClusterer[] WEKA_CLUSTERER;
	static
	{
		WEKA_CLUSTERER = new WekaClusterer[CLUSTERER.length];
		int i = 0;
		for (Clusterer c : CLUSTERER)
			WEKA_CLUSTERER[i++] = new WekaClusterer(c);
	}

	Clusterer wekaClusterer;
	ClusterEvaluation eval;
	List<ClusterData> clusters;

	private WekaClusterer(Clusterer wekaClusterer)
	{
		this.wekaClusterer = wekaClusterer;
	}

	@Override
	public void clusterDataset(String datasetName, String filename, List<CompoundData> compounds,
			List<MoleculeProperty> features)
	{
		File f = CompoundArffWriter.writeArffFile(ListUtil.cast(MolecularPropertyOwner.class, compounds), features);
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(f));
			Instances data = new Instances(reader);

			eval = new ClusterEvaluation();
			wekaClusterer.buildClusterer(data);
			eval.setClusterer(wekaClusterer);
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
			DatasetClustererUtil.storeClusters(filename, wekaClusterer.getClass().getSimpleName(), clusters);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public List<ClusterData> getClusters()
	{
		return clusters;
	}

	@Override
	public boolean requiresNumericalFeatures()
	{
		return true;
	}

	@Override
	public Property[] getProperties()
	{
		return WekaPropertyUtil.getProperties(wekaClusterer);
	}

	@Override
	public void setProperties(Property[] properties)
	{
		WekaPropertyUtil.setProperties(wekaClusterer, properties);
	}

	@Override
	public String getName()
	{
		return wekaClusterer.getClass().getSimpleName() + " (WEKA)";
	}

	@Override
	public String getDescription()
	{
		String s = "A clustering algorithm integrated from the WEKA workbench (version " + Version.VERSION
				+ ", see http://www.cs.waikato.ac.nz/ml/weka).\n\n";
		// weka has no interface for globalInfo
		try
		{
			Method m[] = wekaClusterer.getClass().getDeclaredMethods();
			for (Method method : m)
			{
				if (method.getName().equals("globalInfo"))
				{
					s += method.invoke(wekaClusterer, (Object[]) null).toString();
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

	@Override
	public String getPreconditionErrors()
	{
		return null;
	}
}
