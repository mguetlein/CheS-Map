package org.chesmapper.map.alg.align3d;

import java.io.File;
import java.util.List;

import org.chesmapper.map.alg.AbstractAlgorithm;
import org.chesmapper.map.data.ClusterDataImpl;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.FeatureService;
import org.chesmapper.map.dataInterface.ClusterData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.SubstructureSmartsType;
import org.chesmapper.map.main.Settings;

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
		if (Settings.CACHING_ENABLED && new File(file).exists())
			Settings.LOGGER.debug("aligned file already exists: " + file);
		else
		{
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
		}
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
