package dataInterface;

import java.util.List;

import data.fragments.MatchEngine;

public interface ClusterData extends MolecularPropertyOwner
{
	public String getName();

	public String getFilename();

	public int getSize();

	public List<CompoundData> getCompounds();

	public String getSubstructureSmarts(SubstructureSmartsType type);

	public MatchEngine getSubstructureSmartsMatchEngine(SubstructureSmartsType type);

	public boolean isAligned();

	public String getSummaryStringValue(MoleculeProperty p);

	public int numMissingValues(MoleculeProperty p);

	public String getAlignAlgorithm();

	public boolean containsNotClusteredCompounds();
}
