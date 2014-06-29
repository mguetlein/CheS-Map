package dataInterface;

import data.fragments.MatchEngine;

public class FragmentProperty extends DefaultCompoundProperty
{
	public FragmentProperty(FragmentPropertySet set, String name, String description, String smarts,
			MatchEngine matchEngine)
	{
		//super(name, uniqueName, description);
		super(name, description);
		this.set = set;
		setSmarts(smarts);
		setSmartsMatchEngine(matchEngine);
		setTypeAllowed(Type.NUMERIC, false);
		setType(Type.NOMINAL);
	}

	protected Integer freq;
	protected FragmentPropertySet set;

	public void setFrequency(int f)
	{
		freq = f;
	}

	public int getFrequency()
	{
		return freq;
	}

	@Override
	public FragmentPropertySet getCompoundPropertySet()
	{
		return set;
	}

}
