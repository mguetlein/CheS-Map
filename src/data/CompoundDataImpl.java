package data;

import java.util.HashMap;

import javax.vecmath.Vector3f;

import util.HashMapUtil;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;

public class CompoundDataImpl implements CompoundData
{
	private Vector3f position;
	private int index;
	private HashMap<MoleculeProperty, Object> values = new HashMap<MoleculeProperty, Object>();
	private HashMap<MoleculeProperty, Object> normalizedValues = new HashMap<MoleculeProperty, Object>();

	public Vector3f getPosition()
	{
		return position;
	}

	public void setPosition(Vector3f position)
	{
		this.position = position;
	}

	public int getIndex()
	{
		return index;
	}

	public void setIndex(int index)
	{
		this.index = index;
	}

	public Object getObjectValue(MoleculeProperty p, boolean normalized)
	{
		if (normalized)
			return normalizedValues.get(p);
		else
			return values.get(p);
	}

	public void setValue(MoleculeProperty p, Object v, boolean normalized)
	{
		if (normalized)
			normalizedValues.put(p, v);
		else
			values.put(p, v);
	}

	@Override
	public Double getValue(MoleculeProperty p, boolean normalized)
	{
		return (Double) getObjectValue(p, normalized);
	}

	public String getValuesString(boolean normalized)
	{
		if (normalized)
			return HashMapUtil.toString(values);
		else
			return HashMapUtil.toString(normalizedValues);
	}

}
