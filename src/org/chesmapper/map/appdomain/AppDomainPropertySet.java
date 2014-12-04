package org.chesmapper.map.appdomain;


public class AppDomainPropertySet //extends AbstractCompoundProperty implements CompoundPropertySet
{
	//	private static HashMap<String, AppDomainPropertySet> map = new HashMap<String, AppDomainPropertySet>();
	//
	//	public static AppDomainPropertySet create(String approachName, DatasetFile data, double d[], String uniqNameSuffix)
	//	{
	//		if (!map.containsKey(uniqNameSuffix))
	//			map.put(uniqNameSuffix, new AppDomainPropertySet(approachName, data, d, uniqNameSuffix));
	//		return map.get(uniqNameSuffix);
	//	}
	//
	//	public static AppDomainPropertySet create(String approachName, DatasetFile data, boolean b[], String uniqNameSuffix)
	//	{
	//		if (!map.containsKey(uniqNameSuffix))
	//			map.put(uniqNameSuffix, new AppDomainPropertySet(approachName, data, b, uniqNameSuffix));
	//		return map.get(uniqNameSuffix);
	//	}
	//
	//	private Type type;
	//
	//	private AppDomainPropertySet(String approachName, DatasetFile data, double d[], String uniqNameSuffix)
	//	{
	//		super("numeric " + Settings.text("props.app-domain." + approachName), Settings.text("props.app-domain."
	//				+ approachName + ".desc"));
	//		//		Double s[] = new Double[d.length];
	//		//		for (int i = 0; i < s.length; i++)
	//		//			s[i] = 1 - d[i];
	//		type = Type.NUMERIC;
	//		setDoubleValues(data, ArrayUtil.toDoubleArray(d));
	//	}
	//
	//	private AppDomainPropertySet(String approachName, DatasetFile data, boolean b[], String uniqNameSuffix)
	//	{
	//		super("boolean " + Settings.text("props.app-domain." + approachName), "app-domain." + approachName + "."
	//				+ uniqNameSuffix, Settings.text("props.app-domain." + approachName + ".desc"));
	//		//		Double s[] = new Double[d.length];
	//		//		for (int i = 0; i < s.length; i++)
	//		//			s[i] = 1 - d[i];
	//		type = Type.NOMINAL;
	//		setStringValues(data, ArrayUtil.toStringArray(ArrayUtil.toBooleanArray(b)));
	//	}
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
	//		return type;
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
