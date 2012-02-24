package alg.align3d;

import java.util.ArrayList;
import java.util.List;

import main.Settings;
import alg.AbstractAlgorithm;
import data.ClusterDataImpl;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.MoleculeProperty;
import dataInterface.SubstructureSmartsType;

public class NoAligner extends AbstractAlgorithm implements ThreeDAligner
{
	public static final NoAligner INSTANCE = new NoAligner();

	private NoAligner()
	{
	}

	List<String> clusterFiles;

	@Override
	public String getName()
	{
		return Settings.text("align.no-align");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("align.no-align.desc");
	}

	@Override
	public void algin(DatasetFile dataset, List<ClusterData> clusters, List<MoleculeProperty> features)
	{
		clusterFiles = new ArrayList<String>();
		for (ClusterData c : clusters)
		{
			clusterFiles.add(c.getFilename());
			((ClusterDataImpl) c).setAlignAlgorithm(getName());
		}
	}

	@Override
	public List<String> getAlginedClusterFiles()
	{
		return clusterFiles;
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
