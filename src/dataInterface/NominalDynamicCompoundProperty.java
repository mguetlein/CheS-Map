package dataInterface;


public abstract class NominalDynamicCompoundProperty //extends DynamicCompoundProperty
{
	//	String[] domain;
	//	int[] domainCounts;
	//	String[] stringValues;
	//	String modeNonNull;
	//	Integer missing;
	//	Integer distinct;
	//
	//	public NominalDynamicCompoundProperty(String values[])
	//	{
	//		int miss = 0;
	//		for (Object o : values)
	//			if (o == null)
	//				miss++;
	//		missing = miss;
	//
	//		CountedSet<String> set = CountedSet.create(values);
	//		set.remove(null);
	//		String dom[] = ArrayUtil.toArray(set.values());
	//		int domCounts[] = new int[dom.length];
	//		for (int i = 0; i < domCounts.length; i++)
	//			domCounts[i] = set.getCount(dom[i]);
	//		Arrays.sort(dom, new ToStringComparator());
	//		domain = dom;
	//		domainCounts = domCounts;
	//		distinct = set.getNumValues();
	//		modeNonNull = set.values().get(0);
	//
	//		stringValues = values;
	//
	//	}
	//
	//	@Override
	//	public String getFormattedValueInMappedDataset(Object doubleOrString)
	//	{
	//		return AbstractCompoundProperty.getFormattedValueInMappedDataset(this, doubleOrString);
	//	}
	//
	//	@Override
	//	public String getFormattedValue(Object doubleOrString, DatasetFile dataset)
	//	{
	//		return AbstractCompoundProperty.getFormattedValue(this, doubleOrString, dataset);
	//	}
	//
	//	@Override
	//	public String toString()
	//	{
	//		return getName();
	//	}
	//
	//	@Override
	//	public Type getType()
	//	{
	//		return Type.NOMINAL;
	//	}
	//
	//	@Override
	//	public String[] getNominalDomainInMappedDataset()
	//	{
	//		return domain;
	//	}
	//
	//	@Override
	//	public String[] getNominalDomain(DatasetFile dataset)
	//	{
	//		if (dataset != Settings.MAPPED_DATASET)
	//			throw new IllegalStateException();
	//		return domain;
	//	}
	//
	//	@Override
	//	public int numDistinctValuesInMappedDataset()
	//	{
	//		return distinct;
	//	}
	//
	//	@Override
	//	public int numMissingValuesInMappedDataset()
	//	{
	//		return missing;
	//	}
	//
	//	@Override
	//	public String getModeNonNullInMappedDataset()
	//	{
	//		return modeNonNull;
	//	}
	//
	//	@Override
	//	public String[] getStringValuesInCompleteMappedDataset()
	//	{
	//		return stringValues;
	//	}
	//
	//	@Override
	//	public String[] getStringValues(DatasetFile dataset)
	//	{
	//		if (dataset != Settings.MAPPED_DATASET)
	//			throw new IllegalStateException();
	//		return stringValues;
	//	}

}
