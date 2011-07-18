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

public class ClusterDataImpl implements ClusterData
{
	private String name;
	private String filename;
	private Vector3f position;
	private List<CompoundData> compounds = new ArrayList<CompoundData>();
	private DistanceMatrix<CompoundData> compoundDistances;
	private String substructureSmarts;
	private HashMap<MoleculeProperty, ArraySummary> values = new HashMap<MoleculeProperty, ArraySummary>();
	private HashMap<MoleculeProperty, ArraySummary> normalizedValues = new HashMap<MoleculeProperty, ArraySummary>();

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

	public String getSubstructureSmarts()
	{
		return substructureSmarts;
	}

	public void setSubstructureSmarts(String substructureSmarts)
	{
		this.substructureSmarts = substructureSmarts;
	}

	public ArraySummary getObjectValue(MoleculeProperty p, boolean normalized)
	{
		HashMap<MoleculeProperty, ArraySummary> map;
		if (normalized)
			map = normalizedValues;
		else
			map = values;
		if (map.get(p) == null)
		{
			Object vals[] = new Object[getSize()];
			int i = 0;
			for (CompoundData c : compounds)
				vals[i++] = c.getObjectValue(p, normalized);
			if (p.isNumeric())
				map.put(p, DoubleArraySummary.create(Arrays.asList(vals)));
			else
				map.put(p, CountedSet.create(Arrays.asList(vals)));
		}
		return map.get(p);
	}

	@Override
	public Double getValue(MoleculeProperty p, boolean normalized)
	{
		return ((DoubleArraySummary) getObjectValue(p, normalized)).getMedian();
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
}
