package data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import util.ArraySummary;
import util.CountedSet;
import util.DistanceMatrix;
import util.DoubleArraySummary;
import util.DoubleKeyHashMap;
import util.ListUtil;
import data.fragments.MatchEngine;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;
import dataInterface.SubstructureSmartsType;

public class ClusterDataImpl implements ClusterData
{
	private String name;
	private String alignAlgorithm;
	private boolean isAligned;
	private int origIndex;
	private List<Integer> compoundOrigIndices;
	private List<Integer> compoundClusterIndices = new ArrayList<Integer>();
	private List<CompoundData> compounds = new ArrayList<CompoundData>();
	private DistanceMatrix<CompoundData> compoundDistances;
	private HashMap<SubstructureSmartsType, String> substructureSmarts = new HashMap<SubstructureSmartsType, String>();
	private HashMap<SubstructureSmartsType, MatchEngine> substructureSmartsEngine = new HashMap<SubstructureSmartsType, MatchEngine>();
	private DoubleKeyHashMap<CompoundProperty, String, ArraySummary> values = new DoubleKeyHashMap<CompoundProperty, String, ArraySummary>();
	private boolean containsNotClusteredCompounds = false;
	private List<Integer> origIndicesFilter;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setOrigIndex(int origIndex)
	{
		this.origIndex = origIndex;
	}

	public int getOrigIndex()
	{
		return origIndex;
	}

	@Override
	public boolean isAligned()
	{
		return isAligned;
	}

	public void setAlignAlgorithm(String alignAlgorithm, boolean isAligned)
	{
		this.alignAlgorithm = alignAlgorithm;
		this.isAligned = isAligned;
	}

	@Override
	public String getAlignAlgorithm()
	{
		return alignAlgorithm;
	}

	public List<CompoundData> getCompounds()
	{
		return compounds;
	}

	@Override
	public List<Integer> getCompoundOrigIndices()
	{
		if (compoundOrigIndices == null)
		{
			compoundOrigIndices = new ArrayList<Integer>();
			for (CompoundData c : compounds)
				compoundOrigIndices.add(c.getOrigIndex());
		}
		return compoundOrigIndices;
	}

	@Override
	public void setCompoundClusterIndices(List<Integer> idx)
	{
		this.compoundClusterIndices = idx;
	}

	@Override
	public List<Integer> getCompoundClusterIndices()
	{
		return compoundClusterIndices;
	}

	public void addCompound(CompoundData compound)
	{
		compounds.add(compound);
	}

	@Override
	public String getSubstructureSmarts(SubstructureSmartsType type)
	{
		return substructureSmarts.get(type);
	}

	public void setSubstructureSmarts(SubstructureSmartsType type, String smarts)
	{
		substructureSmarts.put(type, smarts);
	}

	@Override
	public MatchEngine getSubstructureSmartsMatchEngine(SubstructureSmartsType type)
	{
		return substructureSmartsEngine.get(type);
	}

	public void setSubstructureSmartsMatchEngine(SubstructureSmartsType type, MatchEngine engine)
	{
		substructureSmartsEngine.put(type, engine);
	}

	@Override
	public void remove(int indices[])
	{
		Arrays.sort(indices);
		for (int i = indices.length - 1; i >= 0; i--)
			compounds.remove(indices[i]);
		compoundDistances = null;
		values.clear();
	}

	@Override
	public void setFilter(List<Integer> origIndicesFilter)
	{
		this.origIndicesFilter = origIndicesFilter;
	}

	private ArraySummary getSummaryValue(CompoundProperty p)
	{
		return getSummaryValue(p, false);
	}

	private ArraySummary getSummaryValue(CompoundProperty p, boolean formatted)
	{
		String filterKey = (origIndicesFilter == null ? "" : ListUtil.toString(origIndicesFilter)) + formatted;
		if (values.get(p, filterKey) == null)
		{
			if (p.getType() == Type.NUMERIC)
			{
				List<Double> vals = new ArrayList<Double>();
				if (origIndicesFilter == null)
					for (CompoundData c : compounds)
						vals.add(c.getDoubleValue(p));
				else
					for (CompoundData c : compounds)
						if (origIndicesFilter.indexOf(c.getOrigIndex()) != -1)
							vals.add(c.getDoubleValue(p));
				values.put(p, filterKey, DoubleArraySummary.create(vals));
			}
			else
			{
				if (formatted)
				{
					@SuppressWarnings("unchecked")
					CountedSet<String> set = ((CountedSet<String>) getSummaryValue(p, false)).copy();
					for (String key : set.values())
						set.rename(key, p.getFormattedValue(key));
					values.put(p, filterKey, set);
				}
				else
				{
					List<String> vals = new ArrayList<String>();
					if (origIndicesFilter == null)
						for (CompoundData c : compounds)
							vals.add(c.getStringValue(p));
					else
						for (CompoundData c : compounds)
							if (origIndicesFilter.indexOf(c.getOrigIndex()) != -1)
								vals.add(c.getStringValue(p));
					values.put(p, filterKey, CountedSet.create(vals));
				}
			}
		}
		return values.get(p, filterKey);
	}

	public Double getDoubleValue(CompoundProperty p)
	{
		if (p.getType() != Type.NUMERIC)
			throw new IllegalStateException();
		return ((DoubleArraySummary) getSummaryValue(p)).getMean();
	}

	@SuppressWarnings("unchecked")
	public String getStringValue(CompoundProperty p)
	{
		if (p.getType() != Type.NOMINAL)
			throw new IllegalStateException();
		CountedSet<String> set = (CountedSet<String>) getSummaryValue(p);
		String mode = set.getMode(false);
		if (set.getCount(mode) > set.getSum(false) * 2 / 3.0)
			return mode;
		else
			return null;
	}

	@Override
	public String getFormattedValue(CompoundProperty p)
	{
		return getSummaryStringValue(p, false);
	}

	public String getSummaryStringValue(CompoundProperty p, boolean html)
	{
		if (p.getType() == Type.NOMINAL)
			return getSummaryValue(p, true).toString(html);
		if (p.getType() == Type.NUMERIC && p.hasSmallDoubleValuesInMappedDataset())
			return ((DoubleArraySummary) getSummaryValue(p)).toString(html, 3);
		return getSummaryValue(p).toString(html);
	}

	@SuppressWarnings("unchecked")
	public CountedSet<String> getNominalSummary(CompoundProperty p)
	{
		if (p.getType() == Type.NUMERIC)
			throw new IllegalArgumentException();
		else
			return (CountedSet<String>) getSummaryValue(p);
	}

	public int numMissingValues(CompoundProperty p)
	{
		return getSummaryValue(p).getNullCount();
	}

	public DistanceMatrix<CompoundData> getCompoundDistances(List<CompoundProperty> props)
	{
		if (compoundDistances == null)
			compoundDistances = DistanceUtil.computeDistances(compounds, props).cast(CompoundData.class);
		return compoundDistances;
	}

	@Override
	public int getSize()
	{
		return compounds.size();
	}

	@Override
	public boolean containsNotClusteredCompounds()
	{
		return containsNotClusteredCompounds;
	}

	public void setContainsNotClusteredCompounds(boolean b)
	{
		containsNotClusteredCompounds = b;
	}
}
