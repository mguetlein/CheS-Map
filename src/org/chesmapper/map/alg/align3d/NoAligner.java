package org.chesmapper.map.alg.align3d;

import java.util.List;

import org.chesmapper.map.alg.AbstractAlgorithm;
import org.chesmapper.map.data.ClusterDataImpl;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.ClusterData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.SubstructureSmartsType;
import org.chesmapper.map.main.Settings;

public class NoAligner extends AbstractAlgorithm implements ThreeDAligner
{
	public static final NoAligner INSTANCE = new NoAligner();
	private String file;

	private NoAligner()
	{
	}

	public static String getNameStatic()
	{
		return Settings.text("align.no-align");
	}

	@Override
	public String getName()
	{
		return getNameStatic();
	}

	@Override
	public String getDescription()
	{
		return Settings.text("align.no-align.desc");
	}

	@Override
	public void algin(DatasetFile dataset, List<ClusterData> clusters, List<CompoundProperty> features)
	{
		file = dataset.getSDFClustered();
		for (ClusterData c : clusters)
			((ClusterDataImpl) c).setAlignAlgorithm(getName(), false);
	}

	@Override
	public String getAlignedSDFile()
	{
		return file;
	}

	@Override
	public boolean requiresStructuralFragments()
	{
		return false;
	}

	@Override
	public SubstructureSmartsType getSubstructureSmartsType()
	{
		return null;
	}
}
