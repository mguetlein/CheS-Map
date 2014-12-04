package org.chesmapper.map.alg.embed3d;

import org.chesmapper.map.dataInterface.DefaultNumericProperty;
import org.chesmapper.map.main.Settings;

public class CorrelationProperty extends DefaultNumericProperty //implements CompoundPropertySet
{
	//	private static HashMap<String, CorrelationPropertySet> map = new HashMap<String, CorrelationPropertySet>();
	//
	//	public static CorrelationPropertySet create(CorrelationType t, DatasetFile data, double d[], String uniqNameSuffix)
	//	{
	//		if (!map.containsKey(uniqNameSuffix))
	//			map.put(uniqNameSuffix, new CorrelationPropertySet(t, data, d, uniqNameSuffix));
	//		return map.get(uniqNameSuffix);
	//	}
	//
	public CorrelationProperty(CorrelationType t, double d[])
	{
		super(null, Settings.text("props." + t.name().toLowerCase()), Settings.text("props." + t.name().toLowerCase()
				+ ".desc"));
		Double s[] = new Double[d.length];
		for (int i = 0; i < s.length; i++)
			s[i] = 1 - d[i];
		setDoubleValues(s);
	}
	//
	//	@Override
	//	public CompoundPropertySet getCompoundPropertySet()
	//	{
	//		return this;
	//	}
	//
	//	@Override
	//	public boolean isComputed(DatasetFile dataset)
	//	{
	//		return true;
	//	}
	//
	//	@Override
	//	public boolean isCached(DatasetFile dataset)
	//	{
	//		return true;
	//	}
	//
	//	@Override
	//	public boolean compute(DatasetFile dataset)
	//	{
	//		return true;
	//	}
	//
	//	@Override
	//	public boolean isSizeDynamic()
	//	{
	//		return false;
	//	}
	//
	//	@Override
	//	public boolean isSizeDynamicHigh(DatasetFile dataset)
	//	{
	//		return false;
	//	}
	//
	//	@Override
	//	public int getSize(DatasetFile d)
	//	{
	//		return 1;
	//	}
	//
	//	@Override
	//	public CompoundProperty get(DatasetFile d, int index)
	//	{
	//		return this;
	//	}
	//
	//	@Override
	//	public String getDescription()
	//	{
	//		return description;
	//	}
	//
	//	@Override
	//	public Type getType()
	//	{
	//		return Type.NUMERIC;
	//	}
	//
	//	@Override
	//	public Binary getBinary()
	//	{
	//		return null;
	//	}
	//
	//	@Override
	//	public boolean isSelectedForMapping()
	//	{
	//		return false;
	//	}
	//
	//	@Override
	//	public String getNameIncludingParams()
	//	{
	//		return name;
	//	}
	//
	//	@Override
	//	public boolean isComputationSlow()
	//	{
	//		return false;
	//	}
	//
	//	@Override
	//	public boolean isSensitiveTo3D()
	//	{
	//		return false;
	//	}
}
