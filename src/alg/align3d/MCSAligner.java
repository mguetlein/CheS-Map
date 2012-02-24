package alg.align3d;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Messages;
import gui.binloc.Binary;

import java.util.List;

import main.Settings;
import main.TaskProvider;
import util.ExternalToolUtil;
import util.MessageUtil;
import alg.cluster.DatasetClusterer;
import data.ComputeMCS;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.MoleculeProperty;
import dataInterface.SubstructureSmartsType;

public class MCSAligner extends OBFitAligner
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
		return Settings.text("align.mcs.desc", Settings.CDK_STRING, Settings.OPENBABEL_STRING);
	}

	@Override
	public void algin(DatasetFile dataset, List<ClusterData> clusters, List<MoleculeProperty> features)
	{
		ComputeMCS.computeMCS(dataset, clusters);
		if (TaskProvider.task().isCancelled())
			return;
		algin(dataset, clusters, SubstructureSmartsType.MCS);
	}

	@Override
	public SubstructureSmartsType getSubstructureSmartsType()
	{
		return SubstructureSmartsType.MCS;
	}

	public static void main(String args[])
	{
		ExternalToolUtil
				.run("obfit",
						"obfit Oc1ccc(cc1)-c1cocc(:c:c)c1=O /tmp/first4154035072070520801sdf /tmp/remainder4312806650036993699sdf > /tmp/structural_cluster_3.aligned.sdf");
	}

	@Override
	public Binary getBinary()
	{
		return Settings.BABEL_BINARY;
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		if (dataset.numCompounds() >= 50)
			m.add(MessageUtil.slowMessage(Settings.text("align.mcs.slow", MaxFragAligner.getNameStatic())));
		return m;
	}

	@Override
	public boolean requiresStructuralFragments()
	{
		return false;
	}

}
