package dataInterface;

import java.util.HashMap;
import java.util.HashSet;

import util.ArrayUtil;
import data.DatasetFile;

public abstract class AbstractMoleculeProperty implements MoleculeProperty
{
	String name;
	String description;

	Type type;
	HashSet<Type> types = new HashSet<Type>(ArrayUtil.toList(Type.values()));
	Object[] domain;
	protected String smarts;

	public AbstractMoleculeProperty(String name, String description)
	{
		this.name = name;
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

	public void setValues(DatasetFile dataset, Object vals[], boolean numeric)
	{
		if (values.containsKey(dataset))
			throw new IllegalStateException();

		//		System.err.println(toString());
		//		System.err.println(ArrayUtil.toString(vals));
		Double normalized[];
		if (numeric)
			normalized = ArrayUtil.normalize(ArrayUtil.cast(Double.class, vals));
		else
			normalized = ArrayUtil.normalizeObjectArray(vals);
		//		System.err.println(ArrayUtil.toString(normalized));

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
