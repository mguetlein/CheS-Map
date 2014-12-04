package org.chesmapper.map.dataInterface;

public abstract class AbstractCompoundProperty implements CompoundProperty
{
	protected String name;
	protected String description;
	private CompoundProperty isRedundant;
	private Integer missing;
	protected CompoundPropertySet set;

	public AbstractCompoundProperty(CompoundPropertySet set, String name, String description)
	{
		this.set = set;
		this.name = name;
		this.description = description;
	}

	public AbstractCompoundProperty(String name, String description)
	{
		this.name = name;
		this.description = description;
	}

	//	public DefaultCompoundProperty()
	//	{
	//	}

	//	@Override
	//	public String getFormattedValue(Object doubleOrString)
	//	{
	//		return null;
	//	}

	@Override
	public CompoundPropertySet getCompoundPropertySet()
	{
		return set;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String toString()
	{
		return getName();
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public String getFormattedNullValue()
	{
		return "missing";
	}

	protected void setMissing(Object values[])
	{
		int miss = 0;
		for (Object o : values)
			if (o == null)
				miss++;
		missing = miss;
	}

	@Override
	public int numMissingValues()
	{
		return missing;
	}

	@Override
	public CompoundProperty getRedundantProp()
	{
		return isRedundant;
	}

	@Override
	public void setRedundantProp(CompoundProperty b)
	{
		isRedundant = b;
	}
}
