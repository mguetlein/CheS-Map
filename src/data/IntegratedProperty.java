package data;

import gui.binloc.Binary;
import util.DoubleKeyHashMap;
import dataInterface.AbstractMoleculeProperty;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculePropertySet;

public class IntegratedProperty extends AbstractMoleculeProperty implements MoleculePropertySet
{
	String property;
	private boolean usedForMapping;

	private static DoubleKeyHashMap<String, DatasetFile, IntegratedProperty> instances = new DoubleKeyHashMap<String, DatasetFile, IntegratedProperty>();

	private IntegratedProperty(String property, DatasetFile dataset)
	{
		super(property, property + "." + dataset.getShortName() + "." + dataset.getMD5(), "Included in Dataset");
		this.property = property;
	}

	public static IntegratedProperty create(String property, DatasetFile dataset)
	{
		if (!instances.containsKeyPair(property, dataset))
			instances.put(property, dataset, new IntegratedProperty(property, dataset));
		return instances.get(property, dataset);
	}

	public static IntegratedProperty fromString(String property, Type t, DatasetFile dataset)
	{
		IntegratedProperty p = create(property, dataset);
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
	/**
	 * make sure that HashSet<IntegratedPropterty>.get() and .contains() works for two "different" integrated props that return "equals().true"
	 */
	public int hashCode()
	{
		return property.hashCode();
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

	public void setUsedForMapping(boolean usedForMapping)
	{
		this.usedForMapping = usedForMapping;
	}

	@Override
	public boolean isUsedForMapping()
	{
		return usedForMapping;
	}

	@Override
	public Binary getBinary()
	{
		return null;
	}

	@Override
	public String getNameIncludingParams()
	{
		return toString() + "_" + getType();
	}

	@Override
	public boolean isCached(DatasetFile dataset)
	{
		return true;
	}

	@Override
	public boolean isSizeDynamicHigh(DatasetFile dataset)
	{
		return false;
	}

	@Override
	public boolean isComputationSlow()
	{
		return false;
	}
}
