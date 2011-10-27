package alg.align3d;

import gui.binloc.Binary;

import java.util.List;

import main.Settings;
import main.TaskProvider;
import util.ExternalToolUtil;
import alg.Message;
import alg.MessageType;
import alg.cluster.DatasetClusterer;
import data.ComputeMCS;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import dataInterface.SubstructureSmartsType;

public class MCSAligner extends OBFitAligner
{
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
	public Message getMessage(DatasetFile dataset, int numFeatures, Type featureType, boolean smartsFeaturesSelected,
			DatasetClusterer clusterer)
	{
		Message msg = super.getMessage(dataset, numFeatures, featureType, smartsFeaturesSelected, clusterer);
		if (msg != null && msg.getType() == MessageType.Error)
			return msg;
		if (dataset.numCompounds() >= 50)
			return Message.warningMessage("MCS computation is a time consuming task. '"
					+ MaxFragAligner.getNameStatic() + "' should be preferred for large datasets.");
		return msg;
	}

	@Override
	public boolean requiresStructuralFragments()
	{
		return false;
	}

}
