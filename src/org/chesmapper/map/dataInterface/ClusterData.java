package org.chesmapper.map.dataInterface;

import java.util.List;

import org.chesmapper.map.data.fragments.MatchEngine;

public interface ClusterData extends CompoundGroupWithProperties
{
	public String getName();

	public List<Integer> getCompoundOrigIndices();

	public void setCompoundClusterIndices(List<Integer> idx);

	public List<Integer> getCompoundClusterIndices();

	public int getOrigIndex();

	public List<CompoundData> getCompounds();

	public String getSubstructureSmarts(SubstructureSmartsType type);

	public MatchEngine getSubstructureSmartsMatchEngine(SubstructureSmartsType type);

	public boolean isAligned();

	public String getSummaryStringValue(CompoundProperty p, boolean html);

	public int numMissingValues(CompoundProperty p);

	public String getAlignAlgorithm();

	public boolean containsNotClusteredCompounds();

	public void remove(int indices[]);

	public void setFilter(List<Integer> origIndices);

}
