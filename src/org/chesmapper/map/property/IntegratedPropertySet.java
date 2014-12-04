package org.chesmapper.map.property;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.AbstractPropertySet;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.DefaultNominalProperty;
import org.chesmapper.map.dataInterface.DefaultNumericProperty;
import org.chesmapper.map.dataInterface.FragmentProperty.SubstructureType;
import org.mg.javalib.gui.binloc.Binary;
import org.mg.javalib.util.DoubleKeyHashMap;

public class IntegratedPropertySet extends AbstractPropertySet
{
	private String propertyName;
	private boolean selectedForMapping;

	private DefaultNominalProperty propNominal;
	private DefaultNumericProperty propNumeric;

	private IntegratedPropertySet(String property)
	{
		//super(property, /*property + "." + dataset.toString() + "." + dataset.getMD5(),*/"Included in Dataset");
		this.propertyName = property;
		propNominal = new DefaultNominalProperty(this, propertyName, "Included in Dataset");
		propNumeric = new DefaultNumericProperty(this, propertyName, "Included in Dataset");
	}

	private static DoubleKeyHashMap<String, DatasetFile, IntegratedPropertySet> sets = new DoubleKeyHashMap<String, DatasetFile, IntegratedPropertySet>();

	@Override
	public void clearComputedProperties(DatasetFile d)
	{
		sets.removeWithKey2(d);
	}

	public static IntegratedPropertySet create(String property, DatasetFile dataset)
	{
		if (!sets.containsKeyPair(property, dataset))
			sets.put(property, dataset, new IntegratedPropertySet(property));
		return sets.get(property, dataset);
	}

	static IntegratedPropertySet fromString(String property, Type t, DatasetFile dataset)
	{
		IntegratedPropertySet p = create(property, dataset);
		if (!p.isTypeAllowed(t))
			throw new IllegalArgumentException("Type " + t + " is not allowed for prop " + property + " from dataset "
					+ dataset);
		p.setType(t);
		return p;
	}

	@Override
	public String serialize()
	{
		return this + "#" + getType();
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
		return get();
	}

	public CompoundProperty get()
	{
		if (getType() == Type.NUMERIC)
			return propNumeric;
		else
			return propNominal;
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
		return toString().replace(" ", "-") + "_" + getType();
	}

	@Override
	public boolean isCached(DatasetFile dataset)
	{
		return true;
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
	public String getDescription()
	{
		return "Included in Dataset";
	}

	@Override
	public SubstructureType getSubstructureType()
	{
		return null;
	}

	public void setStringValues(String values[])
	{
		propNominal.setStringValues(values);
	}

	public void setDoubleValues(Double values[])
	{
		propNumeric.setDoubleValues(values);
	}

}
