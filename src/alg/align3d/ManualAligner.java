package alg.align3d;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Messages;
import gui.binloc.Binary;
import gui.property.Property;
import gui.property.SelectProperty;
import gui.property.StringProperty;

import java.util.List;

import main.BinHandler;
import main.Settings;
import main.TaskProvider;
import alg.cluster.DatasetClusterer;
import data.ClusterDataImpl;
import data.DatasetFile;
import data.fragments.MatchEngine;
import dataInterface.ClusterData;
import dataInterface.CompoundProperty;
import dataInterface.SmartsUtil;
import dataInterface.SubstructureSmartsType;

public class ManualAligner extends Abstract3DAligner
{
	private ManualAligner()
	{
		super();
	}

	public static ThreeDAligner INSTANCE = new ManualAligner();

	StringProperty smartsProptery = new StringProperty("SMARTS", "");
	SelectProperty matchEngineProperty = new SelectProperty("Chemical library used for alignment",
			MatchEngine.values(), MatchEngine.CDK);

	@Override
	public Property[] getProperties()
	{
		return new Property[] { smartsProptery, matchEngineProperty };
	}

	@Override
	public void algin(DatasetFile dataset, List<ClusterData> clusters, List<CompoundProperty> features)
	{
		for (ClusterData c : clusters)
		{
			((ClusterDataImpl) c).setSubstructureSmarts(SubstructureSmartsType.MANUAL, smartsProptery.getValue());
			((ClusterDataImpl) c).setSubstructureSmartsMatchEngine(SubstructureSmartsType.MANUAL,
					(MatchEngine) matchEngineProperty.getValue());
		}
		alignToSmarts(dataset, clusters, SubstructureSmartsType.MANUAL);
	}

	@Override
	public boolean requiresStructuralFragments()
	{
		return false;
	}

	@Override
	public String getName()
	{
		return Settings.text("align.manual");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("align.manual.desc", Settings.CDK_STRING, Settings.OPENBABEL_STRING);
	}

	@Override
	public SubstructureSmartsType getSubstructureSmartsType()
	{
		return SubstructureSmartsType.MANUAL;
	}

	@Override
	public void giveNoSmartsWarning(int clusterIndex)
	{
		TaskProvider
				.warning(
						"Could not align cluster " + (clusterIndex + 1) + " to manual SMARTS "
								+ smartsProptery.getValue(),
						getName()
								+ " could not align the compounds of this cluster to the SMARTS you have provided. You try using another chemical library for aligning the compounds.");
	}

	@Override
	public Binary getBinary()
	{
		return BinHandler.BABEL_BINARY;
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		if (smartsProptery.getValue() == null || smartsProptery.getValue().length() == 0)
			return Messages.errorMessage("Please provide a SMARTS string");
		int l = SmartsUtil.getLength(smartsProptery.getValue());
		if (l == -1)
			return Messages.errorMessage("Not a valid SMARTS string: '" + smartsProptery.getValue() + "'");
		if (l < 3)
			return Messages.errorMessage("Minimum length for SMARTS is 3");
		if (matchEngineProperty.getValue() == MatchEngine.OpenBabel && !BinHandler.BABEL_BINARY.isFound())
			return Messages.errorMessage("OpenBabel not found, please use CDK for smarts alignment");
		return null;
	}

}
