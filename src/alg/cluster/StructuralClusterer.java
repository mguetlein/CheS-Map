package alg.cluster;

import gui.Progressable;
import gui.property.DoubleProperty;
import gui.property.IntegerProperty;
import gui.property.Property;

import java.util.List;

import main.Settings;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;

public class StructuralClusterer implements DatasetClusterer
{
	List<ClusterData> clusters;

	//	List<int[]> clusterIndices;
	//	String[] smarts;
	double theta = 0.8;
	int minClusterSize = 2;

	@Override
	public String getPreconditionErrors()
	{
		if (Settings.CV_GSPAN_PATH == null)
			return "Graph mining program 'gSpan' not found.";
		else
			return null;
	}

	@Override
	public void clusterDataset(DatasetFile datasetFile, List<CompoundData> compounds, List<MoleculeProperty> features,
			Progressable progress)
	{
		//		clusters = new ArrayList<Cluster>();
		//
		//		String path = "/home/martin/workspace/StructuralClustering";
		//		int saveClusters = -1;
		//		String output = Settings.destinationFile(filename, "/structural_clusterer/");
		//		if (!new File(output).exists())
		//			new File(output).mkdir();
		//		boolean criterionInclusion = true;
		//		boolean useCas = false;
		//		int aromMethod = 2;
		//		boolean gSpanParams = true;
		//
		//		HashMap<Double, MoleculeCluster> bbcClusters = null;
		//		try
		//		{
		//						StructuralClustering sc = new StructuralClustering();
		//						bbcClusters = sc.makeClustering(filename, theta, path, saveClusters, minClusterSize, output,
		//								criterionInclusion, useCas, aromMethod, gSpanParams);
		//		}
		//		catch (Exception e)
		//		{
		//			e.printStackTrace();
		//		}
		//
		//		List<Double> toRemove = new ArrayList<Double>();
		//		for (Double clusterId : bbcClusters.keySet())
		//			if (bbcClusters.get(clusterId).getMolecules().size() < minClusterSize)
		//				toRemove.add(clusterId);
		//		for (Double d : toRemove)
		//			bbcClusters.remove(d);
		//
		//		//		System.out.println("clustering done");
		//		//		for (Double clusterId : bbcClusters.keySet())
		//		//		{
		//		//			MoleculeCluster mc = bbcClusters.get(clusterId);
		//		//			System.out.println("cluster " + clusterId + " " + ArrayUtil.toString(mc.getMolecules().keySet().toArray()));
		//		//		}
		//
		//		//		clusterIndices = new ArrayList<int[]>();
		//		//		smarts = new String[bbcClusters.keySet().size()];
		//
		//		System.out.println("clustering returned " + bbcClusters.keySet().size() + " clusters");
		//
		//		int cCount = 0;
		//		for (Double clusterId : bbcClusters.keySet())
		//		{
		//			System.out.println("clusterid " + clusterId);
		//			ClusterImpl c = new ClusterImpl();
		//
		//			MoleculeCluster mc = bbcClusters.get(clusterId);
		//			HashMap<Double, chemaxon.struc.Molecule> molecules = mc.getMolecules();
		//			for (Double id : molecules.keySet())
		//			{
		//				CompoundImpl cc = new CompoundImpl();
		//				cc.setIndex(id.intValue());
		//				c.addCompound(cc);
		//			}
		//			System.out.println("molecules " + ListUtil.toString(c.getCompounds()));
		//
		//			if (mc.getBackbones().values().size() != 1)
		//				throw new IllegalStateException("num backbones != 1");
		//
		//			c.setSubstructureSmarts(mc.getBackbones().values().iterator().next().toFormat("smiles"));
		//			System.out.println("backbone " + c.getSubstructureSmarts());
		//			clusters.add(c);
		//
		//			cCount++;
		//		}
		//
		//		//		System.exit(0);
		//
		//		storeClusters(filename, "structural");
	}

	//	@Override
	//	public List<int[]> getClusterIndices()
	//	{
	//		return clusterIndices;
	//	}

	public static final String PROPERTY_THETA = "theta (similarity threshold)";
	public static final String PROPERTY_MIN_CLUSTER_SIZE = "min cluster size";

	@Override
	public Property[] getProperties()
	{
		return new Property[] { new DoubleProperty(PROPERTY_THETA, theta),
				new IntegerProperty(PROPERTY_MIN_CLUSTER_SIZE, minClusterSize) };
	}

	@Override
	public void setProperties(Property[] properties)
	{
		for (Property property : properties)
		{
			if (property.getName().equals(PROPERTY_THETA))
				theta = ((DoubleProperty) property).getValue();
			else if (property.getName().equals(PROPERTY_MIN_CLUSTER_SIZE))
				minClusterSize = ((IntegerProperty) property).getValue();
		}
	}

	//	@Override
	//	public String[] getClusterSubstructureSmarts()
	//	{
	//		return smarts;
	//	}

	@Override
	public String getName()
	{
		return "Structural Clusterer";
	}

	@Override
	public String getDescription()
	{
		return "Clusters a dataset into groups that share structural similarity. Groups share a common subgraph of sufficient size.";
	}

	@Override
	public boolean requiresNumericalFeatures()
	{
		return false;
	}

	@Override
	public List<ClusterData> getClusters()
	{
		return clusters;
	}
}
