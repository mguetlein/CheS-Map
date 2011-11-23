package dataInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import util.ArrayUtil;
import util.DoubleArraySummary;
import data.DatasetFile;

public abstract class AbstractMoleculeProperty implements MoleculeProperty
{
	String name;
	private String uniqueName;
	String description;

	Type type;
	HashSet<Type> types = new HashSet<Type>(ArrayUtil.toList(Type.values()));
	String[] domain;
	protected String smarts;

	private static HashMap<String, AbstractMoleculeProperty> uniqueNames = new HashMap<String, AbstractMoleculeProperty>();

	public static void clearPropertyOfType(Class<?> type)
	{
		List<String> toDel = new ArrayList<String>();
		for (String k : uniqueNames.keySet())
			if (uniqueNames.get(k).getClass().equals(type))
				toDel.add(k);
		for (String k : toDel)
			uniqueNames.remove(k);
	}

	public AbstractMoleculeProperty(String name, String description)
	{
		this(name, name, description);
	}

	public AbstractMoleculeProperty(String name, String uniqueName, String description)
	{
		if (uniqueNames.containsKey(uniqueName))
			throw new IllegalArgumentException("Not unique: " + uniqueName);
		uniqueNames.put(uniqueName, this);

		this.name = name;
		this.uniqueName = uniqueName;
		//		if (this.name.length() > 40)
		//		{
		//			if (this.name.endsWith(")") && this.name.indexOf('(') != -1)
		//				this.name = this.name.replaceAll("\\(.*\\)", "").trim();
		//			if (this.name.length() > 40)
		//				this.name = this.name.substring(0, 37) + "...";
		//		}
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
	public String[] getNominalDomain()
	{
		return domain;
	}

	@Override
	public void setNominalDomain(String domain[])
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
	private HashMap<DatasetFile, Double[]> normalizedValues = new HashMap<DatasetFile, Double[]>();
	private HashMap<DatasetFile, Double> median = new HashMap<DatasetFile, Double>();
	private HashMap<DatasetFile, Integer> missing = new HashMap<DatasetFile, Integer>();

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
		//		Double normalized[] = ArrayUtil.normalizeObjectArray(vals);
		setMissing(dataset, vals);
		values.put(dataset, vals);
		//		normalizedValues.put(dataset, normalized);
	}

	public void setDoubleValues(DatasetFile dataset, Double vals[])
	{
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		if (values.containsKey(dataset))
			throw new IllegalStateException();
		Double normalized[] = ArrayUtil.normalize(vals, false);
		setMissing(dataset, normalized);
		//Double normalized[] = ArrayUtil.normalizeLog(vals);
		values.put(dataset, vals);
		normalizedValues.put(dataset, normalized);
		median.put(dataset, DoubleArraySummary.create(normalized).getMedian());
	}

	private void setMissing(DatasetFile dataset, Object values[])
	{
		int miss = 0;
		for (Object o : values)
			if (o == null)
				miss++;
		missing.put(dataset, miss);
	}

	@Override
	public int numMissingValues(DatasetFile dataset)
	{
		return missing.get(dataset);
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
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		if (!normalizedValues.containsKey(dataset))
			throw new Error("values not yet set");
		return normalizedValues.get(dataset);
	}

	@Override
	public Double getNormalizedMedian(DatasetFile dataset)
	{
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		if (!values.containsKey(dataset))
			throw new Error("values not yet set");
		return median.get(dataset);
	}

}
