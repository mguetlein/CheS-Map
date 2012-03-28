package alg.align3d;

import java.util.List;

import main.Settings;
import data.ClusterDataImpl;
import data.DatasetFile;
import data.fragments.MatchEngine;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;
import dataInterface.SmartsUtil;
import dataInterface.SubstructureSmartsType;

public class MaxFragAligner extends Abstract3DAligner
{
	public static final MaxFragAligner INSTANCE = new MaxFragAligner();

	private MaxFragAligner()
	{
	}

	@Override
	public String getName()
	{
		return getNameStatic();
	}

	public static String getNameStatic()
	{
		return Settings.text("align.max-frag");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("align.max-frag.desc", Settings.OPENBABEL_STRING, Settings.CDK_STRING);
	}

	@Override
	public void algin(DatasetFile dataset, List<ClusterData> clusters, List<MoleculeProperty> features)
	{
		for (ClusterData clusterData : clusters)
		{
			MoleculeProperty maxFrag = null;
			int maxFragLength = -1;
			MatchEngine maxMatchEngine = null;
			for (MoleculeProperty feat : features)
			{
				if (!feat.isSmartsProperty())
					continue;
				boolean matchesAll = true;
				for (CompoundData c : clusterData.getCompounds())
					if (c.getStringValue(feat).equals("0"))
					{
						matchesAll = false;
						break;
					}
				if (!matchesAll)
					continue;
				int featLength = SmartsUtil.getLength(feat.getSmarts());
				if (maxFrag == null || maxFragLength < featLength)
				{
					maxFrag = feat;
					maxFragLength = featLength;
					maxMatchEngine = feat.getSmartsMatchEngine();
				}
			}
			if (maxFrag != null)
			{
				((ClusterDataImpl) clusterData).setSubstructureSmarts(SubstructureSmartsType.MAX_FRAG,
						maxFrag.getSmarts());
				((ClusterDataImpl) clusterData).setSubstructureSmartsMatchEngine(SubstructureSmartsType.MAX_FRAG,
						maxMatchEngine);
			}
		}
		alignToSmarts(dataset, clusters, SubstructureSmartsType.MAX_FRAG);
	}

	@Override
	public SubstructureSmartsType getSubstructureSmartsType()
	{
		return SubstructureSmartsType.MAX_FRAG;
	}

	@Override
	public boolean requiresStructuralFragments()
	{
		return true;
	}

}
