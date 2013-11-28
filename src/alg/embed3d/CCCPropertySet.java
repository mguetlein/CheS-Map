package alg.embed3d;

import gui.binloc.Binary;

import java.util.HashMap;

import main.Settings;
import data.DatasetFile;
import dataInterface.AbstractCompoundProperty;
import dataInterface.CompoundProperty;
import dataInterface.CompoundPropertySet;

public class CCCPropertySet extends AbstractCompoundProperty implements CompoundPropertySet
{
	private static HashMap<String, CCCPropertySet> map = new HashMap<String, CCCPropertySet>();

	public static CCCPropertySet create(DatasetFile data, double d[], String uniqNameSuffix)
	{
		if (!map.containsKey(uniqNameSuffix))
			map.put(uniqNameSuffix, new CCCPropertySet(data, d, uniqNameSuffix));
		return map.get(uniqNameSuffix);
	}

	private CCCPropertySet(DatasetFile data, double d[], String uniqNameSuffix)
	{
		super(Settings.text("props.ccc"), "ccc." + uniqNameSuffix, Settings.text("props.ccc.desc"));
		Double s[] = new Double[d.length];
		for (int i = 0; i < s.length; i++)
			s[i] = 1 - d[i];
		setDoubleValues(data, s);
	}

	@Override
	public CompoundPropertySet getCompoundPropertySet()
	{
		return this;
	}

	@Override
	public boolean isComputed(DatasetFile dataset)
	{
		return true;
	}

	@Override
	public boolean isCached(DatasetFile dataset)
	{
		return true;
	}

	@Override
	public boolean compute(DatasetFile dataset)
	{
		return true;
	}

	@Override
	public boolean isSizeDynamic()
	{
		return false;
	}

	@Override
	public boolean isSizeDynamicHigh(DatasetFile dataset)
	{
		return false;
	}

	@Override
	public int getSize(DatasetFile d)
	{
		return 1;
	}

	@Override
	public CompoundProperty get(DatasetFile d, int index)
	{
		return this;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public Type getType()
	{
		return Type.NUMERIC;
	}

	@Override
	public Binary getBinary()
	{
		return null;
	}

	@Override
	public boolean isUsedForMapping()
	{
		return false;
	}

	@Override
	public String getNameIncludingParams()
	{
		return name;
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
}
