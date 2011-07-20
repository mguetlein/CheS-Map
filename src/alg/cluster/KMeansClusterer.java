package alg.cluster;

import gui.Progressable;
import gui.property.IntegerProperty;
import gui.property.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import main.Settings;
import util.ListUtil;
import data.ClusterDataImpl;
import data.CompoundDataImpl;
import data.DatasetFile;
import data.DistanceUtil;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;

/**
 * @deprecated own implementation of kmeans, use weka kmeans clusterer instead
 * @author martin
 *
 */
public class KMeansClusterer implements DatasetClusterer
{

	//	Vector<CDKProperty> validFeatures = new Vector<CDKProperty>();
	//	Vector<Compound> compounds = new Vector<Compound>();

	Vector<Mean> means = new Vector<Mean>();
	List<ClusterData> clusters;

	class Mean extends CompoundDataImpl
	{
		public Mean()
		{
			super(null);
		}

		Vector<CompoundData> assigned = new Vector<CompoundData>();

		double dist(CompoundData c, List<MoleculeProperty> props)
		{
			return DistanceUtil.distance(this, c, props, true);
		}

		/**
		 * @return changed
		 */
		boolean computeAvg(List<MoleculeProperty> props)
		{
			boolean changed = false;
			for (MoleculeProperty p : props)
			{
				double oldVal = getValue(p, true);
				double newVal = 0;
				for (CompoundData c : assigned)
					newVal += c.getValue(p, true);
				newVal /= assigned.size();
				if (Math.abs(newVal - oldVal) > 0.0001)
				{
					changed = true;
					setValue(p, newVal, true);
				}
			}
			return changed;
		}
	}

	int k = 10;
	int randomSeed = 1;

	List<int[]> clusterIndices;

	@Override
	public void clusterDataset(DatasetFile dataset, List<CompoundData> compounds, List<MoleculeProperty> features,
			Progressable progress)
	{
		clusters = new ArrayList<ClusterData>();

		Random random = new Random(randomSeed);
		int numFeatures = features.size();

		System.out.println("kMeans clustering k: " + k + " #features: " + numFeatures);
		if (numFeatures == 0)
			throw new IllegalArgumentException("no features for k-Means clustering");

		for (int i = 0; i < k; i++)
		{
			Mean mean = new Mean();
			for (MoleculeProperty p : features)
				mean.setValue(p, random.nextDouble(), true);
			means.add(mean);
		}

		if (numFeatures == 1)
		{
			final MoleculeProperty sortProp = features.get(0);
			Collections.sort(means, new Comparator<Mean>()
			{
				@Override
				public int compare(Mean o1, Mean o2)
				{
					return o1.getValue(sortProp, true) > o2.getValue(sortProp, true) ? 1 : -1;
				}
			});
		}

		boolean meanValuesChanged = true;
		int count = 0;

		System.out.print("Iteration.. ");
		while (meanValuesChanged)
		{
			count++;
			//			System.out.println("\nk-means iteration");
			System.out.print(count + " ");
			meanValuesChanged = false;

			for (Mean mean : means)
				mean.assigned.clear();

			// assign clusters to mean
			for (CompoundData compound : compounds)
			{
				Mean minMean = null;
				double minDist = Double.MAX_VALUE;
				for (Mean mean : means)
				{
					double d = mean.dist(compound, features);
					if (d < minDist)
					{
						minDist = d;
						minMean = mean;
					}
				}
				minMean.assigned.add(compound);
			}

			// compute mean values
			for (Mean mean : means)
			{
				//				System.out.println("mean values " + ArrayUtil.toString(mean.values) + ", assignments: "
				//						+ mean.assignedCompounds.size());
				if (mean.assigned.size() > 0)
					meanValuesChanged |= mean.computeAvg(features);
			}
		}
		System.out.println(".. done");

		boolean noCompounds[] = new boolean[means.size()];
		for (int i = 0; i < noCompounds.length; i++)
			noCompounds[i] = means.get(i).assigned.size() == 0;
		for (int i = noCompounds.length - 1; i >= 0; i--)
			if (noCompounds[i])
				means.remove(i);

		count = 0;
		System.out.println("Num clusters: " + means.size());
		for (Mean mean : means)
		{
			count++;
			System.out.println(count + " values " + mean.getValuesString(true) + ", size: " + mean.assigned.size());
			if (Settings.DBG)
				System.out.println("  compounds  " + ListUtil.toString(mean.assigned));
		}

		for (Mean m : means)
		{
			ClusterDataImpl c = new ClusterDataImpl();
			for (CompoundData cc : m.assigned)
				c.addCompound(cc);
			clusters.add(c);
		}
		DatasetClustererUtil.storeClusters(dataset.getSDFPath(), "k_means", clusters);
		//
		//		clusterDist = new double[means.size()][means.size()];
		//		for (int i = 0; i < clusterDist.length; i++)
		//			for (int j = 0; j < clusterDist[0].length; j++)
		//				clusterDist[i][j] = means.get(i).dist(means.get(j));
		//		ArrayUtil.normalize(clusterDist);

	}

	//	@Override
	//	public List<int[]> getClusterIndices()
	//	{
	//		if (clusterIndices == null)
	//		{
	//			clusterIndices = new ArrayList<int[]>();
	//			for (Mean m : means)
	//			{
	//				int i[] = new int[m.assignedCompounds.size()];
	//				for (int j = 0; j < i.length; j++)
	//					i[j] = m.assignedCompounds.get(j);
	//				clusterIndices.add(i);
	//			}
	//		}
	//		return clusterIndices;
	//	}

	//	@Override
	//	public String[] getClusterNames()
	//	{
	//		return names;
	//	}
	//
	//	@Override
	//	public String[] getClusterFiles()
	//	{
	//		return files;
	//	}

	//	@Override
	//	public List<double[]> getClusterFeatures()
	//	{
	//		if (clusterFeatures == null)
	//		{
	//			clusterFeatures = new ArrayList<double[]>();
	//			for (Mean m : means)
	//				clusterFeatures.add(m.values);
	//		}
	//		return clusterFeatures;
	//	}

	//	@Override
	//	public double[][] getClusterDistances()
	//	{
	//		if (clusterDistances == null)
	//		{
	//			clusterDistances = new double[means.size()][means.size()];
	//			for (int i = 0; i < clusterDistances.length; i++)
	//				for (int j = 0; j < clusterDistances.length; j++)
	//					clusterDistances[i][j] = means.get(i).dist(means.get(j).values);
	//		}
	//		return clusterDistances;
	//	}

	public static final String PROPERTY_K = "k (Num means)";
	public static final String PROPERTY_RANDOM_SEED = "Random seed";

	@Override
	public Property[] getProperties()
	{
		return new Property[] { new IntegerProperty(PROPERTY_K, k),
				new IntegerProperty(PROPERTY_RANDOM_SEED, randomSeed) };
	}

	@Override
	public void setProperties(Property[] properties)
	{
		for (Property property : properties)
		{
			if (property.getName().equals(PROPERTY_K))
				k = ((IntegerProperty) property).getValue();
			else if (property.getName().equals(PROPERTY_RANDOM_SEED))
				randomSeed = ((IntegerProperty) property).getValue();
		}
	}

	//	double clusterDist[][];
	//
	//	@Override
	//	public String[] getClusterNames()
	//	{
	//		return names;
	//	}
	//
	//	@Override
	//	public String[] getClusterFiles()
	//	{
	//		return files;
	//	}
	//
	//	@Override
	//	public boolean providesDistance()
	//	{
	//		return true;
	//	}
	//
	//	@Override
	//	public double[][] getClusterDistanceMatrix()
	//	{
	//		return clusterDist;
	//	}
	//
	//	@Override
	//	public double[][] getCompoundDistanceMatrix(int cluster)
	//	{
	//		throw new NotImplementedException();
	//	}
	//
	//	@Override
	//	public boolean providesFeatures()
	//	{
	//		return true;
	//	}
	//
	//	String[] featureNames;
	//
	//	@Override
	//	public String[] getFeatureNames()
	//	{
	//		if (featureNames == null)
	//		{
	//			featureNames = new String[validFeatures.size()];
	//			for (int i = 0; i < featureNames.length; i++)
	//				featureNames[i] = validFeatures.get(i).toString();
	//		}
	//		return featureNames;
	//	}
	//
	//	double[][] clusterFeatureValues;
	//
	//	@Override
	//	public double[][] getClusterFeatureValues()
	//	{
	//		if (clusterFeatureValues == null)
	//		{
	//			clusterFeatureValues = new double[means.size()][validFeatures.size()];
	//			for (int j = 0; j < means.size(); j++)
	//				for (int i = 0; i < validFeatures.size(); i++)
	//					clusterFeatureValues[j][i] = means.get(j).values[i];
	//		}
	//		return clusterFeatureValues;
	//	}
	//
	//	HashMap<Integer, double[][]> compoundFeatureValues = new HashMap<Integer, double[][]>();
	//
	//	@Override
	//	public double[][] getCompoundFeatureValues(int cluster)
	//	{
	//		if (compoundFeatureValues.get(cluster) == null)
	//		{
	//			Compound mean = means.get(cluster);
	//			int[] compoundIndices = mean.assignedIndices();
	//
	//			double[][] cfv = new double[compoundIndices.length][validFeatures.size()];
	//
	//			for (int j = 0; j < compoundIndices.length; j++)
	//				for (int i = 0; i < validFeatures.size(); i++)
	//					cfv[j][i] = compounds.get(compoundIndices[j]).values[i];
	//
	//			compoundFeatureValues.put(cluster, cfv);
	//		}
	//		return compoundFeatureValues.get(cluster);
	//	}

	@Override
	public String getName()
	{
		return "k-Means Clusterer";
	}

	@Override
	public String getDescription()
	{
		return "k centroids (data points that represent a cluster) are intialized randomly. The compounds are assigned to the closest centroids. The centroid position and cluster assignments are updated iteratively.";
	}

	@Override
	public boolean requiresNumericalFeatures()
	{
		return true;
	}

	@Override
	public List<ClusterData> getClusters()
	{
		return clusters;
	}

	@Override
	public String getPreconditionErrors()
	{
		return null;
	}

}
