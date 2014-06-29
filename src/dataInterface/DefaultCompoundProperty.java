package dataInterface;

import gui.property.ColorGradient;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashSet;

import util.ArrayUtil;
import util.CountedSet;
import util.DoubleArraySummary;
import util.StringUtil;
import util.ToStringComparator;
import data.fragments.MatchEngine;

public class DefaultCompoundProperty implements CompoundProperty
{
	protected String name;
	//	private String uniqueName;
	protected String description;
	private boolean isSmiles;

	Type type;
	HashSet<Type> types = new HashSet<Type>(ArrayUtil.toList(Type.values()));
	protected String smarts;
	protected MatchEngine matchEngine;

	private boolean logEnabled = false;
	private ColorGradient colorGradient = null;
	private Color[] colorSequence = null;

	//	private static HashMap<String, AbstractCompoundProperty> uniqueNames = new HashMap<String, AbstractCompoundProperty>();
	//
	//	public static void clearPropertyOfType(Class<?> type)
	//	{
	//		List<String> toDel = new ArrayList<String>();
	//		for (String k : uniqueNames.keySet())
	//			if (uniqueNames.get(k).getClass().equals(type))
	//				toDel.add(k);
	//		for (String k : toDel)
	//			uniqueNames.remove(k);
	//	}

	//	public AbstractCompoundProperty(String name, String description)
	//	{
	//		this(name, name, description);
	//	}
	//
	//	public AbstractCompoundProperty(String name, String uniqueName, String description)
	//	{
	//		if (uniqueNames.containsKey(uniqueName))
	//			throw new IllegalArgumentException("Not unique: " + uniqueName);
	//		uniqueNames.put(uniqueName, this);
	//
	//		this.name = name;
	//		this.uniqueName = uniqueName;
	//		//		if (this.name.length() > 40)
	//		//		{
	//		//			if (this.name.endsWith(")") && this.name.indexOf('(') != -1)
	//		//				this.name = this.name.replaceAll("\\(.*\\)", "").trim();
	//		//			if (this.name.length() > 40)
	//		//				this.name = this.name.substring(0, 37) + "...";
	//		//		}
	//		this.description = description;
	//	}

	public DefaultCompoundProperty(String name, String description, String[] values)
	{
		this(name, description);
		this.type = Type.NOMINAL;
		setStringValues(values);
	}

	public DefaultCompoundProperty(String name, String description, Double[] values)
	{
		this(name, description);
		this.type = Type.NUMERIC;
		setDoubleValues(values);
	}

	public DefaultCompoundProperty(String name, String description)
	{
		this.name = name;
		this.description = description;
	}

	public DefaultCompoundProperty()
	{
	}

	@Override
	public CompoundPropertySet getCompoundPropertySet()
	{
		return null;
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

	//	@Override
	//	public String getUniqueName()
	//	{
	//		return uniqueName;
	//	}

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

	private String[] domain;
	private int[] domainCounts;
	private String[] stringValues;
	private Double[] doubleValues;
	private Double[] normalizedValues;
	private Double[] normalizedLogValues;
	private Double median;
	private String modeNonNull;
	private Integer missing;
	private Integer distinct;
	private Boolean isInteger;
	private Boolean hasSmallDoubleValues;
	private CompoundProperty isRedundant;

	@Override
	public boolean isValuesSet()
	{
		if (getType() == Type.NUMERIC)
			return doubleValues != null;
		else
			return stringValues != null;
	}

	public void setStringValues(String vals[])
	{
		if (!isTypeAllowed(Type.NOMINAL))
			throw new IllegalStateException();
		//		if (stringValues.containsKey(dataset))
		//			throw new IllegalStateException();
		//		Double normalized[] = ArrayUtil.normalizeObjectArray(vals);
		setMissing(vals);
		setDomainAndNumDistinct(vals);
		stringValues = vals;
		//		normalizedValues.put(dataset, normalized);
	}

	public void setDoubleValues(Double vals[])
	{
		if (!isTypeAllowed(Type.NUMERIC))
			throw new IllegalStateException();
		//		if (doubleValues.containsKey(dataset))
		//			throw new IllegalStateException();
		vals = ArrayUtil.replaceNaN(vals, null);
		Double normalized[] = ArrayUtil.normalize(vals, false);
		setMissing(normalized);
		Double normalizedLog[] = ArrayUtil.normalizeLog(vals, false);
		doubleValues = vals;
		normalizedValues = normalized;
		normalizedLogValues = normalizedLog;
		DoubleArraySummary sum = DoubleArraySummary.create(normalized);
		median = sum.getMedian();
		distinct = sum.getNumDistinct();
		Boolean integerValue = true;
		for (Double d : vals)
			if (d != null && Math.round(d) != d)
			{
				integerValue = false;
				break;
			}
		isInteger = integerValue;
		Boolean smallDoubleValue = false;
		for (Double d : vals)
			if (d != null && d < 0.005 && d > -0.005)
			{
				smallDoubleValue = true;
				break;
			}
		hasSmallDoubleValues = smallDoubleValue;
	}

	public String getFormattedNullValue()
	{
		return "missing";
	}

	static String getFormattedValue(CompoundProperty p, Object doubleOrString)
	{
		if (doubleOrString == null)
			return p.getFormattedNullValue();
		if (p.getType() == Type.NUMERIC)
		{
			Double d = (Double) doubleOrString;
			if (p.isInteger())
				return StringUtil.formatDouble(d, 0);
			else if (p.hasSmallDoubleValues())
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

	private void setDomainAndNumDistinct(String values[])
	{
		CountedSet<String> set = CountedSet.create(values);
		set.remove(null);
		if (set.getNumValues() == 0)
		{
			domain = new String[0];
			domainCounts = new int[0];
			distinct = 0;
			modeNonNull = null;
		}
		else
		{
			String dom[] = ArrayUtil.toArray(set.values());
			int domCounts[] = new int[dom.length];
			for (int i = 0; i < domCounts.length; i++)
				domCounts[i] = set.getCount(dom[i]);
			Arrays.sort(dom, new ToStringComparator());
			domain = dom;
			domainCounts = domCounts;
			distinct = set.getNumValues();
			modeNonNull = set.values().get(0);
		}
	}

	//	private void checkValuesForDataset(DatasetFile dataset)
	//	{
	//		if (dataset == null)
	//			throw new Error("dataset is null, probably mapped dataset not yet set");
	//		if (!missing.containsKey(dataset))
	//		{
	//			String msg = "dataset " + dataset.getName() + "/" + dataset.hashCode() + " not yet set for property "
	//					+ this + "\ncurrently mapped dataset:" + Settings.MAPPED_DATASET.getName() + "/"
	//					+ Settings.MAPPED_DATASET.hashCode() + "\navailable dataset in proptery: ";
	//			for (DatasetFile d : missing.keySet())
	//				msg += d.getName() + "/" + d.hashCode() + ", ";
	//			throw new Error(msg);
	//		}
	//	}

	@Override
	public String[] getNominalDomain()
	{
		//		checkValuesForDataset(dataset);
		return domain;
	}

	@Override
	public int[] getNominalDomainCounts()
	{
		//		checkValuesForDataset(dataset);
		return domainCounts;
	}

	@Override
	public int numDistinctValuesInCompleteDataset()
	{
		//		checkValuesForDataset(dataset);
		return distinct;
	}

	private void setMissing(Object values[])
	{
		int miss = 0;
		for (Object o : values)
			if (o == null)
				miss++;
		missing = miss;
	}

	@Override
	public int numMissingValuesInCompleteDataset()
	{
		return missing;
	}

	@Override
	public String[] getStringValuesInCompleteDataset()
	{
		if (getType() == Type.NUMERIC)
			throw new IllegalStateException();
		//		checkValuesForDataset(dataset);
		return stringValues;
	}

	@Override
	public Double[] getDoubleValuesInCompleteDataset()
	{
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		//		checkValuesForDataset(dataset);
		return doubleValues;
	}

	@Override
	public Double[] getNormalizedValuesInCompleteDataset()
	{
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		//		checkValuesForDataset(dataset);
		return normalizedValues;//.get(dataset);
	}

	@Override
	public Double[] getNormalizedLogValuesInCompleteDataset()
	{
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		//		checkValuesForDataset(dataset);
		return normalizedLogValues;//.get(dataset);
	}

	@Override
	public Double getNormalizedMedianInCompleteDataset()
	{
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		//		checkValuesForDataset(dataset);
		return median;//.get(dataset);
	}

	@Override
	public Boolean isInteger()
	{
		return isInteger;
	}

	@Override
	public Boolean hasSmallDoubleValues()
	{
		return hasSmallDoubleValues;
	}

	@Override
	public String getModeNonNull()
	{
		if (getType() == Type.NUMERIC)
			throw new IllegalStateException();
		return modeNonNull;
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

	@Override
	public Color[] getHighlightColorSequence()
	{
		if (getType() == Type.NUMERIC)
			throw new IllegalStateException();
		return colorSequence;
	}

	@Override
	public void setHighlightColorSequence(Color[] seq)
	{
		if (getType() == Type.NUMERIC)
			throw new IllegalStateException();
		colorSequence = seq;
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
