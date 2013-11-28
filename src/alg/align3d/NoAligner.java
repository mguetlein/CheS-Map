package alg.align3d;

import java.util.List;

import main.Settings;
import alg.AbstractAlgorithm;
import data.ClusterDataImpl;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.CompoundProperty;
import dataInterface.SubstructureSmartsType;

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
