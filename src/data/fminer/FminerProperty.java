package data.fminer;

import java.util.HashMap;

import data.fragments.MatchEngine;
import dataInterface.AbstractFragmentProperty;
import dataInterface.CompoundPropertySet;

public class FminerProperty extends AbstractFragmentProperty
{
	FminerPropertySet set;

	private FminerProperty(FminerPropertySet set, String smarts)
	{
		super(smarts, "none", smarts, MatchEngine.OpenBabel);
		this.set = set;
	}

	private static HashMap<String, FminerProperty> instances = new HashMap<String, FminerProperty>();

	static FminerProperty create(FminerPropertySet set, String smarts)
	{
		String key = smarts;
		if (!instances.containsKey(key))
			instances.put(key, new FminerProperty(set, smarts));
		return instances.get(key);
	}

	@Override
	public CompoundPropertySet getCompoundPropertySet()
	{
		return set;
	}

}
