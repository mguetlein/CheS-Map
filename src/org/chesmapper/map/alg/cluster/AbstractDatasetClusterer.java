package org.chesmapper.map.alg.cluster;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.chesmapper.map.alg.AbstractAlgorithm;
import org.chesmapper.map.data.ClusterDataImpl;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.ClusterData;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.gui.FeatureWizardPanel.FeatureInfo;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.chesmapper.map.util.ValueFileCache;
import org.chesmapper.map.weka.CascadeSimpleKMeans;
import org.mg.javalib.gui.Message;
import org.mg.javalib.gui.Messages;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.io.SDFUtil;

public abstract class AbstractDatasetClusterer extends AbstractAlgorithm implements DatasetClusterer
{
	private List<ClusterData> clusters;
	protected ClusterApproach clusterApproach = ClusterApproach.Other;
	private String sdf;
	private Boolean multiAssignment;

	@Override
	public ClusterApproach getClusterApproach()
	{
		return clusterApproach;
	}

	@Override
	public final List<ClusterData> getClusters()
	{
		return clusters;
	}

	@Override
	public boolean requiresFeatures()
	{
		return true;
	}

	@Override
	public Property getFixedNumClustersProperty()
	{
		return null;
	}

	@Override
	public Property getDistanceFunctionProperty()
	{
		return null;
	}

	@Override
	public final boolean isDisjointClusterer()
	{
		return !multiAssignment;
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		if (requiresFeatures() && !featureInfo.isFeaturesSelected())
			m.add(Message.errorMessage(Settings.text("error.no-features")));
		else if (getFixedNumClustersProperty() != null)
			m.add(Message.infoMessage(Settings.text("cluster.info.fixed-k", getFixedNumClustersProperty()
					.getDisplayName())));
		return m;
	}

	protected abstract List<Integer[]> cluster(DatasetFile dataset, List<CompoundData> compounds,
			List<CompoundProperty> features) throws Exception;

	protected abstract String getShortName();

	public void clusterDataset(DatasetFile dataset, List<CompoundData> compounds, List<CompoundProperty> features)
			throws Exception
	{
		String filename = dataset.getClusterAssignmentFilePath();
		List<Integer[]> clusterAssignements;

		boolean interactive = false;
		if (this instanceof WekaClusterer && ((WekaClusterer) this).wekaClusterer instanceof CascadeSimpleKMeans)
			for (Property p : getProperties())
				if (p.getName().equals("manuallySelectNumClusters") && (Boolean) p.getValue())
				{
					interactive = true;
					break;
				}

		if (Settings.CACHING_ENABLED && new File(filename).exists() && !interactive)
		{
			Settings.LOGGER.info("Read cached cluster results from: " + filename);
			clusterAssignements = ValueFileCache.readCacheInteger(filename);
		}
		else
		{
			clusterAssignements = cluster(dataset, compounds, features);
			if (!TaskProvider.isRunning())
				return;
			Settings.LOGGER.info("Store cluster results to: " + filename);
			if (!interactive)
				ValueFileCache.writeCacheInteger(filename, clusterAssignements);
		}

		HashMap<Integer, List<Integer>> compoundToCluster = new HashMap<Integer, List<Integer>>();

		multiAssignment = false;
		for (int m = 0; m < clusterAssignements.size(); m++)
		{
			Integer[] clusterIndices = clusterAssignements.get(m);
			if (clusterIndices != null)
			{
				multiAssignment |= clusterIndices.length > 1;

				List<Integer> clusterList = compoundToCluster.get(m);
				if (clusterList == null)
				{
					clusterList = new ArrayList<Integer>();
					compoundToCluster.put(m, clusterList);
				}
				for (Integer c : clusterIndices)
					clusterList.add(c);
			}
		}
		if (multiAssignment)
			TaskProvider.warning(Settings.text("cluster.warning.disjoint"),
					Settings.text("cluster.warning.disjoint.desc"));

		//		if (isDisjointClusterer())
		//			for (int i = 0; i < clusterAssignements.size(); i++)
		//			{
		//				List<Integer> cluster = new ArrayList<Integer>();
		//				cluster.add(clusterAssignements.get(i));
		//				compoundToCluster.put(i, cluster);
		//			}
		//		else
		//		{
		//			boolean clusterFound = true;
		//			int clusterIndex = 0;
		//			while (clusterFound)
		//			{
		//				clusterFound = false;
		//				int clusterMask = ((int) Math.pow(2, clusterIndex));
		//				for (int compoundIndex = 0; compoundIndex < clusterAssignements.size(); compoundIndex++)
		//				{
		//					BinaryFlag flag = new BinaryFlag(clusterAssignements.get(compoundIndex));
		//					if (flag.isSet(clusterMask))
		//					{
		//						List<Integer> clusterList = compoundToCluster.get(compoundIndex);
		//						if (clusterList == null)
		//						{
		//							clusterList = new ArrayList<Integer>();
		//							compoundToCluster.put(compoundIndex, clusterList);
		//						}
		//						clusterList.add(clusterIndex);
		//						clusterFound = true;
		//					}
		//				}
		//				clusterIndex++;
		//			}
		//		}

		//create cluster objects and add compounds to clusters
		clusters = new ArrayList<ClusterData>();
		HashMap<Integer, ClusterDataImpl> map = new HashMap<Integer, ClusterDataImpl>();
		for (int compoundIndex = 0; compoundIndex < clusterAssignements.size(); compoundIndex++)
		{
			if (compoundToCluster.containsKey(compoundIndex))
			{
				for (Integer clusterIndex : compoundToCluster.get(compoundIndex))
				{
					ClusterDataImpl c = map.get(clusterIndex);
					if (c == null)
					{
						c = new ClusterDataImpl();
						clusters.add(c);
						map.put(clusterIndex, c);
					}
					c.addCompound(compounds.get(compoundIndex));
				}
			}
		}

		//remove empty clusters
		List<Integer> toDelete = new ArrayList<Integer>();
		int i = 0;
		for (ClusterData c : clusters)
		{
			if (c.getNumCompounds() == 0)
				toDelete.add(i);
			i++;
		}
		for (int j = toDelete.size() - 1; j >= 0; j--)
			clusters.remove(toDelete.get(j).intValue());

		//add cluster for not-clustered compounds
		ClusterDataImpl unclusteredCompounds = null;
		for (int j = 0; j < compounds.size(); j++)
		{
			if (compoundToCluster.get(j) == null || compoundToCluster.get(j).size() == 0)
			{
				if (unclusteredCompounds == null)
				{
					unclusteredCompounds = new ClusterDataImpl();
					unclusteredCompounds.setContainsNotClusteredCompounds(true);
					clusters.add(unclusteredCompounds);
				}
				unclusteredCompounds.addCompound(compounds.get(j));
			}
		}

		int count = 0;
		for (ClusterData c : clusters)
		{
			if (c.getNumCompounds() == 0)
				throw new Error("empty cluster");
			((ClusterDataImpl) c).setOrigIndex(count);
			((ClusterDataImpl) c).setName(clusterName(count++, c == unclusteredCompounds));
		}

		if (!multiAssignment)
		{
			sdf = dataset.getSDF3D();
			for (ClusterData c : clusters)
			{
				List<Integer> clusterIdx = new ArrayList<Integer>();
				for (CompoundData comp : c.getCompounds())
					clusterIdx.add(comp.getOrigIndex());
				c.setCompoundClusterIndices(clusterIdx);
			}
		}
		else
		{
			//we have to create a new sdf 
			sdf = dataset.getClusterSDFile();
			if (Settings.CACHING_ENABLED && new File(sdf).exists())
				Settings.LOGGER.info("multi-asignment cluster-file exists: " + sdf);
			else
			{
				Settings.LOGGER.info("write compounds to multi-asignment cluster-file");
				for (ClusterData c : clusters)
					SDFUtil.filter(dataset.getSDF3D(), sdf, c.getCompoundOrigIndices(), true, true);
			}
			//update the cluster-indices accordingly
			int cIdx = 0;
			for (ClusterData c : clusters)
			{
				List<Integer> clusterIdx = new ArrayList<Integer>();
				for (int k = 0; k < c.getNumCompounds(); k++)
					clusterIdx.add(cIdx++);
				c.setCompoundClusterIndices(clusterIdx);
			}
		}

		if (unclusteredCompounds != null)
			TaskProvider.warning(Settings.text("cluster.warning.not-clustered-compounds",
					unclusteredCompounds.getName(), unclusteredCompounds.getNumCompounds() + ""), Settings
					.text("cluster.warning.not-clustered-compounds.desc"));

		if (count == 0)
			throw new Error("clusterer returned no cluster");
	}

	@Override
	public String getClusterSDFile()
	{
		return sdf;
	}

	private static String clusterName(int index, boolean notClusteredCompounds)
	{
		if (notClusteredCompounds)
			return "Not clustered";
		else
			return "Cluster " + (index + 1);
	}
}
