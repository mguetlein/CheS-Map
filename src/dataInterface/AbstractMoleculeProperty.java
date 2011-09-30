package dataInterface;

import java.util.HashMap;
import java.util.HashSet;

import util.ArrayUtil;
import data.DatasetFile;

public abstract class AbstractMoleculeProperty implements MoleculeProperty
{
	String name;
	private String uniqueName;
	String description;

	Type type;
	HashSet<Type> types = new HashSet<Type>(ArrayUtil.toList(Type.values()));
	Object[] domain;
	protected String smarts;

	private static HashSet<String> uniqueNames = new HashSet<String>();

	public AbstractMoleculeProperty(String name, String description)
	{
		this(name, name, description);
	}

	public AbstractMoleculeProperty(String name, String uniqueName, String description)
	{
		if (uniqueNames.contains(uniqueName))
			throw new IllegalArgumentException("Not unique: " + uniqueName);
		uniqueNames.add(uniqueName);

		this.name = name;
		this.uniqueName = uniqueName;
		if (this.name.length() > 40)
		{
			if (this.name.endsWith(")") && this.name.indexOf('(') != -1)
				this.name = this.name.replaceAll("\\(.*\\)", "").trim();
			if (this.name.length() > 40)
				this.name = this.name.substring(0, 37) + "...";
		}
		this.description = description;
	}

	@Override
	public String toString()
	{
		return name;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getUniqueName()
	{
		return uniqueName;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public Type getType()
	{
		return type;
	}

	@Override
	public void setType(Type type)
	{
		this.type = type;
	}

	@Override
	public boolean isTypeAllowed(Type type)
	{
		return types.contains(type);
	}

	@Override
	public void setTypeAllowed(Type type, boolean allowed)
	{
		if (allowed)
			types.add(type);
		else if (types.contains(type))
			types.remove(type);
	}

	@Override
	public Object[] getNominalDomain()
	{
		return domain;
	}

	@Override
	public void setNominalDomain(Object domain[])
	{
		this.domain = domain;
	}

	@Override
	public boolean isSmartsProperty()
	{
		return smarts != null && smarts.length() > 0;
	}

	@Override
	public String getSmarts()
	{
		return smarts;
	}

	public void setSmarts(String smarts)
	{
		this.smarts = smarts;
	}

	private HashMap<DatasetFile, Object[]> values = new HashMap<DatasetFile, Object[]>();
	private HashMap<DatasetFile, Object[]> normalizedValues = new HashMap<DatasetFile, Object[]>();

	public boolean isValuesSet(DatasetFile dataset)
	{
		return values.containsKey(dataset);
	}

	public void setStringValues(DatasetFile dataset, String vals[])
	{
		if (getType() == Type.NUMERIC)
			throw new IllegalStateException();
		if (values.containsKey(dataset))
			throw new IllegalStateException();
		Double normalized[] = ArrayUtil.normalizeObjectArray(vals);
		values.put(dataset, vals);
		normalizedValues.put(dataset, normalized);
	}

	public void setDoubleValues(DatasetFile dataset, Double vals[])
	{
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		if (values.containsKey(dataset))
			throw new IllegalStateException();
		Double normalized[] = ArrayUtil.normalize(vals);
		values.put(dataset, vals);
		normalizedValues.put(dataset, normalized);
	}

	@Override
	public String[] getStringValues(DatasetFile dataset)
	{
		if (getType() == Type.NUMERIC)
			throw new IllegalStateException();
		if (!values.containsKey(dataset))
			throw new Error("values not yet set");
		return ArrayUtil.cast(String.class, values.get(dataset));
	}

	@Override
	public Double[] getDoubleValues(DatasetFile dataset)
	{
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		if (!values.containsKey(dataset))
			throw new Error("values not yet set");
		return ArrayUtil.parse(values.get(dataset));
	}

	@Override
	public Double[] getNormalizedValues(DatasetFile dataset)
	{
		if (!normalizedValues.containsKey(dataset))
			throw new Error("values not yet set");
		return ArrayUtil.cast(Double.class, normalizedValues.get(dataset));
	}

}
