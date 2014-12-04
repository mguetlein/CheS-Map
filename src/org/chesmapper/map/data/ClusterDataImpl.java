package org.chesmapper.map.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.chesmapper.map.data.fragments.MatchEngine;
import org.chesmapper.map.dataInterface.ClusterData;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.dataInterface.SubstructureSmartsType;
import org.mg.javalib.util.ArraySummary;
import org.mg.javalib.util.CountedSet;
import org.mg.javalib.util.DistanceMatrix;
import org.mg.javalib.util.DoubleArraySummary;
import org.mg.javalib.util.DoubleKeyHashMap;
import org.mg.javalib.util.ListUtil;

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
			if (p instanceof NumericProperty)
			{
				NumericProperty np = (NumericProperty) p;
				List<Double> vals = new ArrayList<Double>();
				if (origIndicesFilter == null)
					for (CompoundData c : compounds)
						vals.add(c.getDoubleValue(np));
				else
					for (CompoundData c : compounds)
						if (origIndicesFilter.indexOf(c.getOrigIndex()) != -1)
							vals.add(c.getDoubleValue(np));
				values.put(p, filterKey, DoubleArraySummary.create(vals));
			}
			else
			{
				NominalProperty np = (NominalProperty) p;
				if (formatted)
				{
					@SuppressWarnings("unchecked")
					CountedSet<String> set = ((CountedSet<String>) getSummaryValue(p, false)).copy();
					for (String key : set.values())
						set.rename(key, np.getFormattedValue(key));
					set.setToBack(p.getFormattedNullValue());
					values.put(p, filterKey, set);
				}
				else
				{
					List<String> vals = new ArrayList<String>();
					if (origIndicesFilter == null)
						for (CompoundData c : compounds)
							vals.add(c.getStringValue(np));
					else
						for (CompoundData c : compounds)
							if (origIndicesFilter.indexOf(c.getOrigIndex()) != -1)
								vals.add(c.getStringValue(np));
					values.put(p, filterKey, CountedSet.create(vals));
				}
			}
		}
		return values.get(p, filterKey);
	}

	public Double getDoubleValue(NumericProperty p)
	{
		if (origIndicesFilter != null && origIndicesFilter.size() == 0)
			return null;
		if (getSummaryValue(p).isAllNull())
			return null;
		return ((DoubleArraySummary) getSummaryValue(p)).getMean();
	}

	@Override
	public String getFormattedValue(CompoundProperty p)
	{
		return getSummaryStringValue(p, false);
	}

	public String getSummaryStringValue(CompoundProperty p, boolean html)
	{
		if (origIndicesFilter != null && origIndicesFilter.size() == 0)
			return null;
		if (p instanceof NominalProperty)
			return getSummaryValue(p, true).toString(html);
		if (p instanceof NumericProperty && ((NumericProperty) p).hasSmallDoubleValues())
			return ((DoubleArraySummary) getSummaryValue(p)).toString(html, 3);
		return getSummaryValue(p).toString(html);
	}

	@SuppressWarnings("unchecked")
	public CountedSet<String> getNominalSummary(NominalProperty p)
	{
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
	public int getNumCompounds()
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
