package data;

import dataInterface.MoleculeProperty;

public class SDFProperty implements MoleculeProperty
{
	String property;
	boolean numeric = false;

	public SDFProperty(String property)
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
		return (o instanceof SDFProperty) && ((SDFProperty) o).property.equals(property);
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
