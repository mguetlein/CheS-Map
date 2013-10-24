package dataInterface;

import java.util.List;

import util.CountedSet;
import data.fragments.MatchEngine;

public interface ClusterData extends CompoundPropertyOwner
{
	public String getName();

	public String getFilename();

	public int getOrigIndex();

	public int getSize();

	public List<CompoundData> getCompounds();

	public String getSubstructureSmarts(SubstructureSmartsType type);

	public MatchEngine getSubstructureSmartsMatchEngine(SubstructureSmartsType type);

	public boolean isAligned();

	public String getSummaryStringValue(CompoundProperty p, boolean html);

	public CountedSet<String> getNominalSummary(CompoundProperty p);

	public int numMissingValues(CompoundProperty p);

	public String getAlignAlgorithm();

	public boolean containsNotClusteredCompounds();

	public void remove(int indices[]);
}
