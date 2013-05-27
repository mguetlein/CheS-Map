package data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import util.ArraySummary;
import util.CountedSet;
import util.DistanceMatrix;
import util.DoubleArraySummary;
import util.HashMapUtil;
import util.ListUtil;
import data.fragments.MatchEngine;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.CompoundPropertyOwner;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;
import dataInterface.SubstructureSmartsType;

public class ClusterDataImpl implements ClusterData
{
	private String name;
	private String alignAlgorithm;
	private String filename;
	private String alignedFilename;
	private List<CompoundData> compounds = new ArrayList<CompoundData>();
	private DistanceMatrix<CompoundData> compoundDistances;
	private HashMap<SubstructureSmartsType, String> substructureSmarts = new HashMap<SubstructureSmartsType, String>();
	private HashMap<SubstructureSmartsType, MatchEngine> substructureSmartsEngine = new HashMap<SubstructureSmartsType, MatchEngine>();
	private HashMap<CompoundProperty, ArraySummary> values = new HashMap<CompoundProperty, ArraySummary>();
	private HashMap<CompoundProperty, ArraySummary> normalizedValues = new HashMap<CompoundProperty, ArraySummary>();
	private boolean containsNotClusteredCompounds = false;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getFilename()
	{
		if (isAligned())
			return alignedFilename;
		else
			return filename;
	}

	public void setFilename(String filename)
	{
		this.filename = filename;
	}

	public void setAlignedFilename(String alignedFilename)
	{
		this.alignedFilename = alignedFilename;
	}

	@Override
	public boolean isAligned()
	{
		return alignedFilename != null;
	}

	public void setAlignAlgorithm(String alignAlgorithm)
	{
		this.alignAlgorithm = alignAlgorithm;
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

	public List<Integer> calculateCompoundIndices()
	{
		List<Integer> indices = new ArrayList<Integer>();
		for (CompoundData c : compounds)
			indices.add(c.getIndex());
		return indices;
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

	private ArraySummary getSummaryValue(CompoundProperty p, boolean normalized)
	{
		if (p.getType() != Type.NUMERIC && normalized)
			throw new IllegalStateException();

		HashMap<CompoundProperty, ArraySummary> map;
		if (normalized)
			map = normalizedValues;
		else
			map = values;
		if (map.get(p) == null)
		{
			if (p.getType() == Type.NUMERIC || normalized)
			{
				Double vals[] = new Double[getSize()];
				int i = 0;
				for (CompoundData c : compounds)
					if (normalized)
						vals[i++] = c.getNormalizedValue(p);
					else
						vals[i++] = c.getDoubleValue(p);
				map.put(p, DoubleArraySummary.create(Arrays.asList(vals)));
			}
			else
			{
				String vals[] = new String[getSize()];
				int i = 0;
				for (CompoundData c : compounds)
					vals[i++] = c.getStringValue(p);
				map.put(p, CountedSet.create(Arrays.asList(vals)));
			}
		}
		return map.get(p);
	}

	public Double getDoubleValue(CompoundProperty p)
	{
		if (p.getType() != Type.NUMERIC)
			throw new IllegalStateException();
		return ((DoubleArraySummary) getSummaryValue(p, false)).getMedian();
	}

	@SuppressWarnings("unchecked")
	public String getStringValue(CompoundProperty p)
	{
		if (p.getType() != Type.NOMINAL)
			throw new IllegalStateException();
		return ((CountedSet<String>) getSummaryValue(p, false)).values().get(0);
	}

	public Double getNormalizedValue(CompoundProperty p)
	{
		return ((DoubleArraySummary) getSummaryValue(p, true)).getMedian();
	}

	public String getSummaryStringValue(CompoundProperty p)
	{
		return getSummaryValue(p, false).toString();
	}

	public int numMissingValues(CompoundProperty p)
	{
		return getSummaryValue(p, false).getNumNull();
	}

	public DistanceMatrix<CompoundData> getCompoundDistances(List<CompoundProperty> props)
	{
		if (compoundDistances == null)
			compoundDistances = DistanceUtil.computeDistances(ListUtil.cast(CompoundPropertyOwner.class, compounds),
					props).cast(CompoundData.class);
		return compoundDistances;
	}

	public String getValuesString(boolean normalized)
	{
		if (normalized)
			return HashMapUtil.toString(values);
		else
			return HashMapUtil.toString(normalizedValues);
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
