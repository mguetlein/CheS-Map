package alg.align3d;

import java.util.List;

import main.Settings;
import alg.AbstractAlgorithm;
import data.ClusterDataImpl;
import data.DatasetFile;
import data.FeatureService;
import dataInterface.ClusterData;
import dataInterface.CompoundProperty;
import dataInterface.SubstructureSmartsType;

public class BigDataFakeAligner extends AbstractAlgorithm implements ThreeDAligner
{
	public static final BigDataFakeAligner INSTANCE = new BigDataFakeAligner();
	private String file;

	private BigDataFakeAligner()
	{
	}

	public static String getNameStatic()
	{
		return Settings.text("align.big-data-fake-align");
	}

	@Override
	public String getName()
	{
		return getNameStatic();
	}

	@Override
	public String getDescription()
	{
		return Settings.text("align.big-data-fake-align.desc");
	}

	@Override
	public void algin(DatasetFile dataset, List<ClusterData> clusters, List<CompoundProperty> features)
	{
		file = dataset.getAlignSDFilePath();
		int idx[] = new int[dataset.numCompounds()];
		for (int i = 0; i < idx.length; i++)
			idx[i] = i;
		try
		{
			FeatureService.writeOrigCompoundsToSDFile(dataset.getCompounds(), file, idx, true, true);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		dataset.getSDFClustered();
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
