package data;

import gui.binloc.Binary;

import java.util.HashMap;

import dataInterface.AbstractMoleculeProperty;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculePropertySet;

public class IntegratedProperty extends AbstractMoleculeProperty implements MoleculePropertySet
{
	String property;
	private boolean usedForMapping;

	private static HashMap<String, IntegratedProperty> instances = new HashMap<String, IntegratedProperty>();

	private IntegratedProperty(String property)
	{
		super(property, "Included in Dataset");
		this.property = property;
	}

	public static IntegratedProperty create(String property)
	{
		if (!instances.containsKey(property))
			instances.put(property, new IntegratedProperty(property));
		return instances.get(property);
	}

	public static IntegratedProperty fromString(String property, Type t)
	{
		IntegratedProperty p = create(property);
		if (!p.isTypeAllowed(t))
			throw new IllegalArgumentException();
		p.setType(t);
		return p;
	}

	@Override
	public String toString()
	{
		return property;
	}

	@Override
	public boolean equals(Object o)
	{
		return (o instanceof IntegratedProperty) && ((IntegratedProperty) o).property.equals(property);
	}

	@Override
	public int getSize(DatasetFile dataset)
	{
		return 1;
	}

	@Override
	public MoleculeProperty get(DatasetFile dataset, int index)
	{
		if (index != 0)
			throw new Error("only one prop available");
		return this;
	}

	@Override
	public MoleculePropertySet getMoleculePropertySet()
	{
		return this;
	}

	@Override
	public boolean isSizeDynamic()
	{
		return false;
	}

	@Override
	public boolean isComputed(DatasetFile dataset)
	{
		return true;
	}

	@Override
	public boolean compute(DatasetFile dataset)
	{
		//do noting
		return true;
	}

	@Override
	public Binary getBinary()
	{
		return null;
	}

	public void setUsedForMapping(boolean usedForMapping)
	{
		this.usedForMapping = usedForMapping;
	}

	@Override
	public boolean isUsedForMapping()
	{
		return usedForMapping;
	}
}
