package alg.embed3d;

import gui.binloc.Binary;

import java.util.HashMap;

import main.Settings;
import data.DatasetFile;
import dataInterface.AbstractCompoundProperty;
import dataInterface.CompoundProperty;
import dataInterface.CompoundPropertySet;

public class CorrelationPropertySet extends AbstractCompoundProperty implements CompoundPropertySet
{
	private static HashMap<String, CorrelationPropertySet> map = new HashMap<String, CorrelationPropertySet>();

	public static CorrelationPropertySet create(CorrelationType t, DatasetFile data, double d[], String uniqNameSuffix)
	{
		if (!map.containsKey(uniqNameSuffix))
			map.put(uniqNameSuffix, new CorrelationPropertySet(t, data, d, uniqNameSuffix));
		return map.get(uniqNameSuffix);
	}

	private CorrelationPropertySet(CorrelationType t, DatasetFile data, double d[], String uniqNameSuffix)
	{
		super(Settings.text("props." + t.name().toLowerCase()), t.name().toLowerCase() + "." + uniqNameSuffix, Settings
				.text("props." + t.name().toLowerCase() + ".desc"));
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
	public boolean isSelectedForMapping()
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
