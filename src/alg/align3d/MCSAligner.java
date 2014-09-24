package alg.align3d;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Messages;

import java.util.List;

import main.Settings;
import main.TaskProvider;
import util.ExternalToolUtil;
import util.MessageUtil;
import alg.cluster.DatasetClusterer;
import data.ComputeMCS;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.CompoundProperty;
import dataInterface.SubstructureSmartsType;

public class MCSAligner extends Abstract3DAligner
{
	public static final MCSAligner INSTANCE = new MCSAligner();

	private MCSAligner()
	{
	}

	@Override
	public String getName()
	{
		return Settings.text("align.mcs");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("align.mcs.desc", Settings.CDK_STRING);
	}

	@Override
	public void algin(DatasetFile dataset, List<ClusterData> clusters, List<CompoundProperty> features)
	{
		ComputeMCS.computeMCS(dataset, clusters);
		if (!TaskProvider.isRunning())
			return;
		alignToSmarts(dataset, clusters);
	}

	@Override
	public SubstructureSmartsType getSubstructureSmartsType()
	{
		return SubstructureSmartsType.MCS;
	}

	public static void main(String args[])
	{
		ExternalToolUtil.run("obfit", new String[] { "obfit", "Oc1ccc(cc1)-c1cocc(:c:c)c1=O",
				"/tmp/first4154035072070520801sdf", "/tmp/remainder4312806650036993699sdf", ">",
				"/tmp/structural_cluster_3.aligned.sdf" });
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		if (Settings.BIG_DATA)
			return Messages.warningMessage(Settings.text("align.warn.ignored-because-big-data"));
		else
		{
			Messages m = super.getMessages(dataset, featureInfo, clusterer);
			if (dataset.numCompounds() >= 50)
				m.add(MessageUtil.slowRuntimeMessage(Settings.text("align.mcs.slow", MaxFragAligner.getNameStatic())));
			return m;
		}
	}

	@Override
	public boolean requiresStructuralFragments()
	{
		return false;
	}

	@Override
	public void giveNoSmartsWarning(int clusterIndex)
	{
		TaskProvider.warning("Could not align cluster " + (clusterIndex + 1) + ", no MCS found.", getName()
				+ " could not align the cluster, as there exists no common subgraph (of size >=" + MIN_NUM_ATOMS
				+ "). The cluster is too structurally diverse.");
	}

}
