package data;

import java.util.ArrayList;
import java.util.List;

import dataInterface.AbstractMoleculeProperty;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculePropertySet;

public class IntegratedProperty extends AbstractMoleculeProperty implements MoleculePropertySet
{
	String property;

	private static List<IntegratedProperty> instances = new ArrayList<IntegratedProperty>();

	private IntegratedProperty(String property)
	{
		this.property = property;
	}

	public static IntegratedProperty create(String property)
	{
		IntegratedProperty p = new IntegratedProperty(property);
		if (instances.indexOf(p) == -1)
			instances.add(p);
		else
			p = instances.get(instances.indexOf(p));
		return p;
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
	public int hashCode()
	{
		return property.hashCode();
	}

	@Override
	public int getSize()
	{
		return 1;
	}

	@Override
	public MoleculeProperty get(int index)
	{
		if (index != 0)
			throw new Error("only one prop available");
		return this;
	}

	@Override
	public String getDescription()
	{
		return toString();
	}
}
