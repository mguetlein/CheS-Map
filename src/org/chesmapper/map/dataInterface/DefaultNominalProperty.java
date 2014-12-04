package org.chesmapper.map.dataInterface;

import java.awt.Color;
import java.util.Arrays;

import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.CountedSet;
import org.mg.javalib.util.ToStringComparator;

public class DefaultNominalProperty extends AbstractCompoundProperty implements NominalProperty
{
	private Color[] colorSequence = null;
	private String[] domain;
	private int[] domainCounts;
	private String[] stringValues;
	private String modeNonNull;
	private Integer distinct;
	private String activeValue;

	public DefaultNominalProperty(String name, String description, String[] values)
	{
		super(name, description);
		setStringValues(values);
	}

	public DefaultNominalProperty(CompoundPropertySet set, String name, String description)
	{
		super(set, name, description);
	}

	@Override
	public boolean isValuesSet()
	{
		return stringValues != null;
	}

	public void setStringValues(String vals[])
	{
		setMissing(vals);
		setDomainAndNumDistinct(vals);
		stringValues = vals;
	}

	@Override
	public String getActiveValue()
	{
		return activeValue;
	}

	@Override
	public String getFormattedValue(String s)
	{
		if (s == null)
			return getFormattedNullValue();
		return s;
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
			{
				domCounts[i] = set.getCount(dom[i]);
				if (dom.length == 2 && dom[i].matches("(?i)active|1|true"))
					activeValue = dom[i];
			}
			Arrays.sort(dom, new ToStringComparator());
			domain = dom;
			domainCounts = domCounts;
			distinct = set.getNumValues();
			modeNonNull = set.values().get(0);
		}
	}

	@Override
	public String[] getDomain()
	{
		return domain;
	}

	@Override
	public int[] getDomainCounts()
	{
		return domainCounts;
	}

	@Override
	public int numDistinctValues()
	{
		return distinct;
	}

	@Override
	public String[] getStringValues()
	{
		return stringValues;
	}

	@Override
	public String getModeNonNull()
	{
		return modeNonNull;
	}

	@Override
	public Color[] getHighlightColorSequence()
	{
		return colorSequence;
	}

	@Override
	public void setHighlightColorSequence(Color[] seq)
	{
		colorSequence = seq;
	}

	@Override
	public boolean isUndefined()
	{
		return getCompoundPropertySet() != null && getCompoundPropertySet().getType() == null;
	}
}
