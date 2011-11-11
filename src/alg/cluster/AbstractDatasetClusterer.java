package alg.cluster;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Message;
import gui.Messages;
import io.SDFUtil;

import java.util.List;

import main.Settings;
import main.TaskProvider;
import alg.AbstractAlgorithm;
import data.ClusterDataImpl;
import data.DatasetFile;
import dataInterface.ClusterData;

public abstract class AbstractDatasetClusterer extends AbstractAlgorithm implements DatasetClusterer
{
	protected List<ClusterData> clusters;

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

	protected void storeClusters(String sdfFile, String clusterFilePrefix, String clusterAlgName,
			Iterable<ClusterData> clusters)
	{
		TaskProvider.task().verbose("Storing cluster results in files");

		int count = 0;
		for (ClusterData c : clusters)
		{
			if (c.getSize() == 0)
				throw new Error("try to store empty cluster");

			//String parent = origFile.substring(0, origFile.lastIndexOf("/"));
			//String origName = origFile.substring(origFile.lastIndexOf("/") + 1);

			String name = clusterFilePrefix + "_cluster_" + count++ + ".sdf";
			String clusterFile = Settings.destinationFile(sdfFile, name);

			// already loaded file may be overwritten, clear
			DatasetFile.clearFilesWith3DSDF(clusterFile);

			SDFUtil.filter(sdfFile, clusterFile, ((ClusterDataImpl) c).calculateCompoundIndices());
			((ClusterDataImpl) c).setName("Cluster " + count);
			((ClusterDataImpl) c).setFilename(clusterFile);
		}
		if (count == 0)
			throw new Error("clusterer returned no cluster");

	}
}
