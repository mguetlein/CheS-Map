package data.integrated;

import gui.binloc.Binary;
import util.DoubleKeyHashMap;
import data.DatasetFile;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.SubstructureType;
import dataInterface.CompoundProperty.Type;
import dataInterface.CompoundPropertySet;

public class IntegratedPropertySet implements CompoundPropertySet
{
	private String propertyName;
	private boolean selectedForMapping;
	private IntegratedProperty prop;

	private IntegratedPropertySet(String property)
	{
		//super(property, /*property + "." + dataset.toString() + "." + dataset.getMD5(),*/"Included in Dataset");
		this.propertyName = property;
		prop = new IntegratedProperty(property);
	}

	private static DoubleKeyHashMap<String, DatasetFile, IntegratedPropertySet> sets = new DoubleKeyHashMap<String, DatasetFile, IntegratedPropertySet>();

	public static IntegratedPropertySet create(String property, DatasetFile dataset)
	{
		if (!sets.containsKeyPair(property, dataset))
			sets.put(property, dataset, new IntegratedPropertySet(property));
		return sets.get(property, dataset);
	}

	public static IntegratedPropertySet fromString(String property, Type t, DatasetFile dataset)
	{
		IntegratedPropertySet p = create(property, dataset);
		if (!p.prop.isTypeAllowed(t))
			throw new IllegalArgumentException();
		p.prop.setType(t);
		return p;
	}

	@Override
	public String toString()
	{
		return propertyName;
	}

	@Override
	public boolean equals(Object o)
	{
		return (o instanceof IntegratedPropertySet) && ((IntegratedPropertySet) o).propertyName.equals(propertyName);
	}

	@Override
	/**
	 * make sure that HashSet<IntegratedPropterty>.get() and .contains() works for two "different" integrated props that return "equals().true"
	 */
	public int hashCode()
	{
		return propertyName.hashCode();
	}

	@Override
	public int getSize(DatasetFile dataset)
	{
		return 1;
	}

	@Override
	public CompoundProperty get(DatasetFile dataset, int index)
	{
		if (index != 0)
			throw new Error("only one prop available");
		return prop;
	}

	public IntegratedProperty get()
	{
		return prop;
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

	public void setSelectedForMapping(boolean selectedForMapping)
	{
		this.selectedForMapping = selectedForMapping;
	}

	@Override
	public boolean isSelectedForMapping()
	{
		return selectedForMapping;
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

	@Override
	public boolean isSensitiveTo3D()
	{
		return false;
	}

	@Override
	public Type getType()
	{
		return prop.getType();
	}

	@Override
	public String getDescription()
	{
		return "Included in Dataset";
	}

	@Override
	public SubstructureType getSubstructureType()
	{
		return null;
	}

}
