package dataInterface;

import gui.property.ColorGradient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import main.Settings;
import util.ArrayUtil;
import util.CountedSet;
import util.DoubleArraySummary;
import util.StringUtil;
import util.ToStringComparator;
import data.DatasetFile;
import data.fragments.MatchEngine;

public abstract class AbstractCompoundProperty implements CompoundProperty
{
	protected String name;
	private String uniqueName;
	protected String description;
	private boolean isSmiles;

	Type type;
	HashSet<Type> types = new HashSet<Type>(ArrayUtil.toList(Type.values()));
	protected String smarts;
	protected MatchEngine matchEngine;

	private boolean logEnabled = false;
	private ColorGradient colorGradient = null;

	private static HashMap<String, AbstractCompoundProperty> uniqueNames = new HashMap<String, AbstractCompoundProperty>();

	public static void clearPropertyOfType(Class<?> type)
	{
		List<String> toDel = new ArrayList<String>();
		for (String k : uniqueNames.keySet())
			if (uniqueNames.get(k).getClass().equals(type))
				toDel.add(k);
		for (String k : toDel)
			uniqueNames.remove(k);
	}

	public AbstractCompoundProperty(String name, String description)
	{
		this(name, name, description);
	}

	public AbstractCompoundProperty(String name, String uniqueName, String description)
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
	public SubstructureType getSubstructureType()
	{
		return getCompoundPropertySet().getSubstructureType();
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
	public boolean isSmiles()
	{
		return isSmiles;
	}

	public void setSmiles(boolean isSmiles)
	{
		this.isSmiles = isSmiles;
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

	@Override
	public MatchEngine getSmartsMatchEngine()
	{
		return matchEngine;
	}

	public void setSmartsMatchEngine(MatchEngine matchEngine)
	{
		this.matchEngine = matchEngine;
	}

	private HashMap<DatasetFile, String[]> domain = new HashMap<DatasetFile, String[]>();
	private HashMap<DatasetFile, int[]> domainCounts = new HashMap<DatasetFile, int[]>();
	private HashMap<DatasetFile, String[]> stringValues = new HashMap<DatasetFile, String[]>();
	private HashMap<DatasetFile, Double[]> doubleValues = new HashMap<DatasetFile, Double[]>();
	private HashMap<DatasetFile, Double[]> normalizedValues = new HashMap<DatasetFile, Double[]>();
	private HashMap<DatasetFile, Double[]> normalizedLogValues = new HashMap<DatasetFile, Double[]>();
	private HashMap<DatasetFile, Double> median = new HashMap<DatasetFile, Double>();
	private HashMap<DatasetFile, String> modeNonNull = new HashMap<DatasetFile, String>();
	private HashMap<DatasetFile, Integer> missing = new HashMap<DatasetFile, Integer>();
	private HashMap<DatasetFile, Integer> distinct = new HashMap<DatasetFile, Integer>();
	private HashMap<DatasetFile, Boolean> isInteger = new HashMap<DatasetFile, Boolean>();
	private HashMap<DatasetFile, Boolean> hasSmallDoubleValues = new HashMap<DatasetFile, Boolean>();

	public boolean isValuesSet(DatasetFile dataset)
	{
		if (getType() == Type.NUMERIC)
			return doubleValues.containsKey(dataset);
		else
			return stringValues.containsKey(dataset);
	}

	public void setStringValues(DatasetFile dataset, String vals[])
	{
		if (!isTypeAllowed(Type.NOMINAL))
			throw new IllegalStateException();
		if (stringValues.containsKey(dataset))
			throw new IllegalStateException();
		//		Double normalized[] = ArrayUtil.normalizeObjectArray(vals);
		setMissing(dataset, vals);
		setDomainAndNumDistinct(dataset, vals);
		stringValues.put(dataset, vals);
		//		normalizedValues.put(dataset, normalized);
	}

	public void setDoubleValues(DatasetFile dataset, Double vals[])
	{
		if (!isTypeAllowed(Type.NUMERIC))
			throw new IllegalStateException();
		if (doubleValues.containsKey(dataset))
			throw new IllegalStateException();
		vals = ArrayUtil.replaceNaN(vals, null);
		Double normalized[] = ArrayUtil.normalize(vals, false);
		setMissing(dataset, normalized);
		Double normalizedLog[] = ArrayUtil.normalizeLog(vals, false);
		doubleValues.put(dataset, vals);
		normalizedValues.put(dataset, normalized);
		normalizedLogValues.put(dataset, normalizedLog);
		DoubleArraySummary sum = DoubleArraySummary.create(normalized);
		median.put(dataset, sum.getMedian());
		distinct.put(dataset, sum.getNumDistinct());
		Boolean integerValue = true;
		for (Double d : vals)
			if (d != null && Math.round(d) != d)
			{
				integerValue = false;
				break;
			}
		isInteger.put(dataset, integerValue);
		Boolean smallDoubleValue = false;
		for (Double d : vals)
			if (d != null && d < 0.005 && d > -0.005)
			{
				smallDoubleValue = true;
				break;
			}
		hasSmallDoubleValues.put(dataset, smallDoubleValue);
	}

	static String getFormattedValue(CompoundProperty p, Object doubleOrString)
	{
		if (doubleOrString == null)
			return "missing";
		if (p.getType() == Type.NUMERIC)
		{
			Double d = (Double) doubleOrString;
			if (p.isIntegerInMappedDataset())
				return StringUtil.formatDouble(d, 0);
			else if (p.hasSmallDoubleValuesInMappedDataset())
				return StringUtil.formatDouble(d, 3);
			else
				return StringUtil.formatDouble(d);
		}
		else
		{
			String s = (String) doubleOrString;
			if (p.isSmartsProperty() || CompoundPropertyUtil.isExportedFPProperty(p))
				return s.equals("1") ? "match" : "no-match";
			//			if (p.toString().contains("activityoutcome"))
			//				return s.equals("1") ? "active" : (s.equals("0") ? "inactive" : s);
			else
				return s + "";
		}
	}

	@Override
	public String getFormattedValue(Object doubleOrString)
	{
		return getFormattedValue(this, doubleOrString);
	}

	private void setDomainAndNumDistinct(DatasetFile dataset, String values[])
	{
		CountedSet<String> set = CountedSet.create(values);
		set.remove(null);
		if (set.getNumValues() == 0)
		{
			domain.put(dataset, new String[0]);
			domainCounts.put(dataset, new int[0]);
			distinct.put(dataset, 0);
			modeNonNull.put(dataset, null);
		}
		else
		{
			String dom[] = ArrayUtil.toArray(set.values());
			int domCounts[] = new int[dom.length];
			for (int i = 0; i < domCounts.length; i++)
				domCounts[i] = set.getCount(dom[i]);
			Arrays.sort(dom, new ToStringComparator());
			domain.put(dataset, dom);
			domainCounts.put(dataset, domCounts);
			distinct.put(dataset, set.getNumValues());
			modeNonNull.put(dataset, set.values().get(0));
		}
	}

	@Override
	public String[] getNominalDomain(DatasetFile dataset)
	{
		if (!domain.containsKey(dataset))
			throw new Error("values not yet set");
		return domain.get(dataset);
	}

	@Override
	public int[] getNominalDomainCounts(DatasetFile dataset)
	{
		if (!domainCounts.containsKey(dataset))
			throw new Error("values not yet set");
		return domainCounts.get(dataset);
	}

	@Override
	public String[] getNominalDomainInMappedDataset()
	{
		return getNominalDomain(Settings.MAPPED_DATASET);
	}

	@Override
	public int numDistinctValues(DatasetFile dataset)
	{
		if (distinct.containsKey(dataset))
			return distinct.get(dataset);
		else
			return -1;
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
		if (!stringValues.containsKey(dataset))
			throw new Error("values not yet set");
		return stringValues.get(dataset);
	}

	@Override
	public Double[] getDoubleValues(DatasetFile dataset)
	{
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		if (!doubleValues.containsKey(dataset))
			throw new Error("values not yet set");
		return doubleValues.get(dataset);
	}

	@Override
	public Double[] getDoubleValuesInCompleteMappedDataset()
	{
		return getDoubleValues(Settings.MAPPED_DATASET);
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
	public Double[] getNormalizedValuesInCompleteMappedDataset()
	{
		return getNormalizedValues(Settings.MAPPED_DATASET);
	}

	@Override
	public Double[] getNormalizedLogValues(DatasetFile dataset)
	{
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		if (!normalizedLogValues.containsKey(dataset))
			throw new Error("values not yet set");
		return normalizedLogValues.get(dataset);
	}

	@Override
	public Double getNormalizedMedian(DatasetFile dataset)
	{
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		if (!doubleValues.containsKey(dataset))
			throw new Error("values not yet set");
		return median.get(dataset);
	}

	@Override
	public Boolean isIntegerInMappedDataset()
	{
		return isInteger(Settings.MAPPED_DATASET);
	}

	@Override
	public Boolean isInteger(DatasetFile dataset)
	{
		if (isInteger.containsKey(dataset))
			return isInteger.get(dataset);
		else
			return null;
	}

	@Override
	public Boolean hasSmallDoubleValues(DatasetFile dataset)
	{
		if (hasSmallDoubleValues.containsKey(dataset))
			return hasSmallDoubleValues.get(dataset);
		else
			return null;
	}

	@Override
	public Boolean hasSmallDoubleValuesInMappedDataset()
	{
		return hasSmallDoubleValues(Settings.MAPPED_DATASET);
	}

	@Override
	public String getModeNonNull(DatasetFile dataset)
	{
		if (getType() == Type.NUMERIC)
			throw new IllegalStateException();
		if (!modeNonNull.containsKey(dataset))
			throw new Error("values not yet set");
		return modeNonNull.get(dataset);
	}

	@Override
	public boolean isLogHighlightingEnabled()
	{
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		return logEnabled;
	}

	@Override
	public void setLogHighlightingEnabled(boolean log)
	{
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		this.logEnabled = log;
	}

	@Override
	public ColorGradient getHighlightColorGradient()
	{
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		return colorGradient;
	}

	@Override
	public void setHighlightColorGradient(ColorGradient colorGradient)
	{
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		this.colorGradient = colorGradient;
	}

}
