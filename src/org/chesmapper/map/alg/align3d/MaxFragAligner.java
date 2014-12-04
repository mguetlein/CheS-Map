package org.chesmapper.map.alg.align3d;

import java.util.List;

import org.chesmapper.map.data.ClusterDataImpl;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.fragments.MatchEngine;
import org.chesmapper.map.dataInterface.ClusterData;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.FragmentProperty;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.SmartsUtil;
import org.chesmapper.map.dataInterface.SubstructureSmartsType;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;

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
	public void algin(DatasetFile dataset, List<ClusterData> clusters, List<CompoundProperty> features)
	{
		for (ClusterData clusterData : clusters)
		{
			FragmentProperty maxFrag = null;
			int maxFragLength = -1;
			MatchEngine maxMatchEngine = null;
			for (CompoundProperty feat : features)
			{
				if (!(feat instanceof FragmentProperty))
					continue;
				boolean matchesAll = true;
				for (CompoundData c : clusterData.getCompounds())
					if (c.getStringValue((NominalProperty) feat).equals("0"))
					{
						matchesAll = false;
						break;
					}
				if (!matchesAll)
					continue;
				int featLength = SmartsUtil.getLength(((FragmentProperty) feat).getSmarts());
				if (featLength >= MIN_NUM_ATOMS && (maxFrag == null || maxFragLength < featLength))
				{
					maxFrag = (FragmentProperty) feat;
					maxFragLength = featLength;
					maxMatchEngine = maxFrag.getSmartsMatchEngine();
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
		alignToSmarts(dataset, clusters);
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

	@Override
	public void giveNoSmartsWarning(int clusterIndex)
	{
		TaskProvider.warning("Could not align cluster " + (clusterIndex + 1) + ", no common fragment found.", getName()
				+ " could not align the cluster, as there is no structural fragment (of size >=" + MIN_NUM_ATOMS
				+ ") that matches all compounds of the cluster. "
				+ "The reason maybe that the cluster is too structurally diverse. "
				+ "You could try to increase the number of structural fragments.");
	}

}
