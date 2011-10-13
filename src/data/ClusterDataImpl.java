package data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.vecmath.Vector3f;

import util.ArraySummary;
import util.CountedSet;
import util.DistanceMatrix;
import util.DoubleArraySummary;
import util.HashMapUtil;
import util.ListUtil;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import dataInterface.SubstructureSmartsType;

public class ClusterDataImpl implements ClusterData
{
	private String name;
	private String alignAlgorithm;
	private String filename;
	private Vector3f position;
	private List<CompoundData> compounds = new ArrayList<CompoundData>();
	private DistanceMatrix<CompoundData> compoundDistances;
	private HashMap<SubstructureSmartsType, String> substructureSmarts = new HashMap<SubstructureSmartsType, String>();
	private HashMap<MoleculeProperty, ArraySummary> values = new HashMap<MoleculeProperty, ArraySummary>();
	private HashMap<MoleculeProperty, ArraySummary> normalizedValues = new HashMap<MoleculeProperty, ArraySummary>();
	private boolean aligned = false;

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
		return filename;
	}

	public void setFilename(String filename)
	{
		this.filename = filename;
	}

	public Vector3f getPosition()
	{
		return position;
	}

	public void setPosition(Vector3f position)
	{
		this.position = position;
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

	private ArraySummary getSummaryValue(MoleculeProperty p, boolean normalized)
	{
		HashMap<MoleculeProperty, ArraySummary> map;
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

	public Double getDoubleValue(MoleculeProperty p)
	{
		if (p.getType() != Type.NUMERIC)
			throw new IllegalStateException();
		return ((DoubleArraySummary) getSummaryValue(p, false)).getMedian();
	}

	@SuppressWarnings("unchecked")
	public String getStringValue(MoleculeProperty p)
	{
		if (p.getType() != Type.NOMINAL)
			throw new IllegalStateException();
		return ((CountedSet<String>) getSummaryValue(p, false)).values().get(0);
	}

	public double getNormalizedValue(MoleculeProperty p)
	{
		return ((DoubleArraySummary) getSummaryValue(p, true)).getMedian();
	}

	public String getSummaryStringValue(MoleculeProperty p)
	{
		return getSummaryValue(p, false).toString();
	}

	public DistanceMatrix<CompoundData> getCompoundDistances(List<MoleculeProperty> props)
	{
		if (compoundDistances == null)
			compoundDistances = DistanceUtil.computeDistances(ListUtil.cast(MolecularPropertyOwner.class, compounds),
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

	public void setAligned(boolean b)
	{
		this.aligned = b;
	}

	@Override
	public boolean isAligned()
	{
		return aligned;
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

}
