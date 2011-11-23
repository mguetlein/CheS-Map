package data;

import java.util.HashMap;

import javax.vecmath.Vector3f;

import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;

public class CompoundDataImpl implements CompoundData
{
	private Vector3f position;
	private int index;
	private HashMap<MoleculeProperty, String> stringValues = new HashMap<MoleculeProperty, String>();
	private HashMap<MoleculeProperty, Double> doubleValues = new HashMap<MoleculeProperty, Double>();
	private HashMap<MoleculeProperty, Double> normalizedValues = new HashMap<MoleculeProperty, Double>();
	private String smiles;

	public CompoundDataImpl(String smiles)
	{
		this.smiles = smiles;
	}

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

	public void setDoubleValue(MoleculeProperty p, Double v)
	{
		doubleValues.put(p, v);
	}

	public void setStringValue(MoleculeProperty p, String v)
	{
		stringValues.put(p, v);
	}

	public void setNormalizedValue(MoleculeProperty p, Double v)
	{
		normalizedValues.put(p, v);
	}

	public Double getDoubleValue(MoleculeProperty p)
	{
		if (p.getType() != Type.NUMERIC)
			throw new IllegalStateException();
		return doubleValues.get(p);
	}

	public String getStringValue(MoleculeProperty p)
	{
		if (p.getType() == Type.NUMERIC)
			throw new IllegalStateException();
		return stringValues.get(p);
	}

	public Double getNormalizedValue(MoleculeProperty p)
	{
		if (p.getType() != Type.NUMERIC)
			throw new IllegalStateException();
		return normalizedValues.get(p);
	}

	@Override
	public String getSmiles()
	{
		return smiles;
	}

}
