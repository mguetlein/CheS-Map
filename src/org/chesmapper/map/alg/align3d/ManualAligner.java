package org.chesmapper.map.alg.align3d;

import java.util.List;

import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.data.ClusterDataImpl;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.fragments.MatchEngine;
import org.chesmapper.map.dataInterface.ClusterData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.SmartsUtil;
import org.chesmapper.map.dataInterface.SubstructureSmartsType;
import org.chesmapper.map.gui.FeatureWizardPanel.FeatureInfo;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.mg.javalib.gui.Messages;
import org.mg.javalib.gui.binloc.Binary;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.gui.property.SelectProperty;
import org.mg.javalib.gui.property.StringProperty;

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
		alignToSmarts(dataset, clusters);
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
		if (Settings.BIG_DATA)
			return Messages.warningMessage(Settings.text("align.warn.ignored-because-big-data"));
		else
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

}
