package alg.cluster;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Message;
import gui.Messages;
import gui.property.PropertyUtil;
import io.SDFUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.Settings;
import main.TaskProvider;
import util.ValueFileCache;
import alg.AbstractAlgorithm;
import data.ClusterDataImpl;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculePropertyUtil;

public abstract class AbstractDatasetClusterer extends AbstractAlgorithm implements DatasetClusterer
{
	private List<ClusterData> clusters;

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
	public String getFixedNumClustersProperty()
	{
		return null;
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		if (requiresFeatures() && !featureInfo.featuresSelected)
			m.add(Message.errorMessage(Settings.text("error.no-features")));
		else if (getFixedNumClustersProperty() != null)
			m.add(Message.infoMessage(Settings.text("cluster.info.fixed-k", getFixedNumClustersProperty())));
		return m;
	}

	protected abstract List<Integer> cluster(DatasetFile dataset, List<CompoundData> compounds,
			List<MoleculeProperty> features) throws Exception;

	protected abstract String getShortName();

	public void clusterDataset(DatasetFile dataset, List<CompoundData> compounds, List<MoleculeProperty> features)
			throws Exception
	{
		String filename = Settings.destinationFile(
				dataset,
				dataset.getShortName()
						+ "."
						+ getShortName()
						+ "."
						+ MoleculePropertyUtil.getSetMD5(features,
								dataset.getMD5() + " " + PropertyUtil.getPropertyMD5(getProperties())) + ".cluster");
		List<Integer> clusterAssignements;

		if (new File(filename).exists())
		{
			System.out.println("read cached cluster results from: " + filename);
			clusterAssignements = ValueFileCache.readCacheInteger2(filename);
		}
		else
		{
			clusterAssignements = cluster(dataset, compounds, features);
			System.out.println("store cluster results to: " + filename);
			ValueFileCache.writeCacheInteger2(filename, clusterAssignements);
		}

		clusters = new ArrayList<ClusterData>();
		HashMap<Integer, ClusterDataImpl> map = new HashMap<Integer, ClusterDataImpl>();
		for (int i = 0; i < clusterAssignements.size(); i++)
		{
			if (!map.containsKey(clusterAssignements.get(i)))
			{
				ClusterDataImpl c = new ClusterDataImpl();
				clusters.add(c);
				map.put(clusterAssignements.get(i), c);
			}
			ClusterDataImpl c = map.get(clusterAssignements.get(i));
			c.addCompound(compounds.get(i));
		}

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

		TaskProvider.task().verbose("Storing cluster results in files");
		int count = 0;
		for (ClusterData c : clusters)
		{
			if (c.getSize() == 0)
				throw new Error("try to store empty cluster");

			//String parent = origFile.substring(0, origFile.lastIndexOf("/"));
			//String origName = origFile.substring(origFile.lastIndexOf("/") + 1);

			String name = dataset.getShortName() + "_" + getShortName() + "_"
					+ PropertyUtil.getPropertyMD5(getProperties()) + "_cluster_" + count++ + ".sdf";
			String clusterFile = Settings.destinationFile(dataset, name);
			if (!new File(clusterFile).exists())
			{
				// already loaded file may be overwritten, clear
				DatasetFile.clearFilesWith3DSDF(clusterFile);
				SDFUtil.filter(dataset.getSDFPath(true), clusterFile, ((ClusterDataImpl) c).calculateCompoundIndices());
			}
			else
				System.out.println("cluster already stored: " + clusterFile);
			((ClusterDataImpl) c).setName("Cluster " + count);
			((ClusterDataImpl) c).setFilename(clusterFile);
		}
		if (count == 0)
			throw new Error("clusterer returned no cluster");
	}
}
