package data;

import dataInterface.MoleculeProperty;

public class IntegratedProperty implements MoleculeProperty
{
	String property;
	boolean numeric = false;

	public IntegratedProperty(String property)
	{
		this.property = property;
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
	public boolean isNumeric()
	{
		return numeric;
	}

	public void setNumeric(boolean numeric)
	{
		this.numeric = numeric;
	}
}
