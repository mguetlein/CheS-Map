package data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import util.ArraySummary;
import util.CountedSet;
import util.DistanceMatrix;
import util.DoubleArraySummary;
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
	private int origIndex;
	private String filename;
	private String alignedFilename;
	private List<CompoundData> compounds = new ArrayList<CompoundData>();
	private DistanceMatrix<CompoundData> compoundDistances;
	private HashMap<SubstructureSmartsType, String> substructureSmarts = new HashMap<SubstructureSmartsType, String>();
	private HashMap<SubstructureSmartsType, MatchEngine> substructureSmartsEngine = new HashMap<SubstructureSmartsType, MatchEngine>();
	private HashMap<CompoundProperty, ArraySummary> values = new HashMap<CompoundProperty, ArraySummary>();
	private boolean containsNotClusteredCompounds = false;

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

	@Override
	public void remove(int indices[])
	{
		Arrays.sort(indices);
		for (int i = indices.length - 1; i >= 0; i--)
			compounds.remove(indices[i]);
		compoundDistances = null;
		values.clear();
	}

	private ArraySummary getSummaryValue(CompoundProperty p)
	{
		if (values.get(p) == null)
		{
			if (p.getType() == Type.NUMERIC)
			{
				Double vals[] = new Double[getSize()];
				int i = 0;
				for (CompoundData c : compounds)
					vals[i++] = c.getDoubleValue(p);
				values.put(p, DoubleArraySummary.create(Arrays.asList(vals)));
			}
			else
			{
				String vals[] = new String[getSize()];
				int i = 0;
				for (CompoundData c : compounds)
					vals[i++] = c.getStringValue(p);
				values.put(p, CountedSet.create(Arrays.asList(vals)));
			}
		}
		return values.get(p);
	}

	public Double getDoubleValue(CompoundProperty p)
	{
		if (p.getType() != Type.NUMERIC)
			throw new IllegalStateException();
		return ((DoubleArraySummary) getSummaryValue(p)).getMedian();
	}

	@SuppressWarnings("unchecked")
	public String getStringValue(CompoundProperty p)
	{
		if (p.getType() != Type.NOMINAL)
			throw new IllegalStateException();
		return ((CountedSet<String>) getSummaryValue(p)).values().get(0);
	}

	@Override
	public String getFormattedValue(CompoundProperty p)
	{
		return getSummaryStringValue(p, false);
	}

	public String getSummaryStringValue(CompoundProperty p, boolean html)
	{
		if (p.isSmartsProperty())
		{
			CountedSet<String> set = getNominalSummary(p).copy();
			if (set.contains("1"))
				set.rename("1", "match");
			if (set.contains("0"))
				set.rename("0", "no-match");
			return set.toString(html);
			//			return "matches " + set.getCount("1") + "/" + set.sum();
		}
		else
		{
			if (p.getType() == Type.NUMERIC && p.hasSmallDoubleValuesInMappedDataset())
				return ((DoubleArraySummary) getSummaryValue(p)).toString(html, 3);
			else
				return getSummaryValue(p).toString(html);
		}

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
		return getSummaryValue(p).getNumNull();
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
