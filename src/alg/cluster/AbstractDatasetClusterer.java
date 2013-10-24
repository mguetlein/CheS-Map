package alg.cluster;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Message;
import gui.Messages;
import gui.property.Property;
import gui.property.PropertyUtil;
import io.SDFUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.Settings;
import main.TaskProvider;
import util.ArrayUtil;
import util.ValueFileCache;
import weka.CascadeSimpleKMeans;
import alg.AbstractAlgorithm;
import data.ClusterDataImpl;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundPropertyUtil;

public abstract class AbstractDatasetClusterer extends AbstractAlgorithm implements DatasetClusterer
{
	private List<ClusterData> clusters;
	protected ClusterApproach clusterApproach = ClusterApproach.Other;

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
	public boolean isDisjointClusterer()
	{
		return true;
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		if (requiresFeatures() && !featureInfo.featuresSelected)
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
		String datasetFeaturesClusterpropsMD5 = CompoundPropertyUtil.getSetMD5(features, dataset.getMD5() + " "
				+ PropertyUtil.getPropertyMD5(getProperties()));

		String filename = Settings.destinationFile(dataset, dataset.getShortName() + "." + getShortName() + "."
				+ datasetFeaturesClusterpropsMD5 + ".cluster");
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
			Settings.LOGGER.info("Store cluster results to: " + filename);
			if (!interactive)
				ValueFileCache.writeCacheInteger(filename, clusterAssignements);
		}

		HashMap<Integer, List<Integer>> compoundToCluster = new HashMap<Integer, List<Integer>>();

		boolean multiAssignment = false;
		for (int m = 0; m < clusterAssignements.size(); m++)
		{
			Integer[] clusterIndices = clusterAssignements.get(m);
			if (clusterIndices != null)
			{
				if (isDisjointClusterer() && clusterIndices.length > 1)
					throw new Error("Disjoint clusterer with more than one cluster assingment "
							+ ArrayUtil.toString(clusterIndices));
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
			if (c.getSize() == 0)
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

		TaskProvider.verbose("Storing cluster results in files");
		int count = 0;
		for (ClusterData c : clusters)
		{
			if (c.getSize() == 0)
				throw new Error("try to store empty cluster");

			// String parent = origFile.substring(0, origFile.lastIndexOf("/"));
			// String origName = origFile.substring(origFile.lastIndexOf("/") + 1);

			String name = dataset.getShortName() + "_" + getShortName() + "_" + datasetFeaturesClusterpropsMD5
					+ "_cluster_" + count + ".sdf";
			String clusterFile = Settings.destinationFile(dataset, name);
			if (!Settings.CACHING_ENABLED || !new File(clusterFile).exists() || interactive)
			{
				// already loaded file may be overwritten, clear
				DatasetFile.clearFilesWith3DSDF(clusterFile);
				SDFUtil.filter(dataset.getSDFPath(true), clusterFile, ((ClusterDataImpl) c).calculateCompoundIndices(),
						true);
			}
			else
				Settings.LOGGER.info("cluster already stored: " + clusterFile);
			((ClusterDataImpl) c).setOrigIndex(count);
			((ClusterDataImpl) c).setName(clusterName(count++, c == unclusteredCompounds));
			((ClusterDataImpl) c).setFilename(clusterFile);
		}
		if (unclusteredCompounds != null)
			TaskProvider.warning(Settings.text("cluster.warning.not-clustered-compounds",
					unclusteredCompounds.getName(), unclusteredCompounds.getSize() + ""), Settings
					.text("cluster.warning.not-clustered-compounds.desc"));

		if (count == 0)
			throw new Error("clusterer returned no cluster");
	}

	private static String clusterName(int index, boolean notClusteredCompounds)
	{
		if (notClusteredCompounds)
			return "Not clustered";
		else
			return "Cluster " + (index + 1);
	}
}
