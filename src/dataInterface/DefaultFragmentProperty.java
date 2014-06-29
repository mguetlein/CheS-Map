package dataInterface;

import data.fragments.MatchEngine;

public class DefaultFragmentProperty extends DefaultNominalProperty implements FragmentProperty
{
	protected String smarts;
	protected MatchEngine matchEngine;
	protected int frequency;

	public DefaultFragmentProperty(FragmentPropertySet set, String name, String description, String smarts,
			MatchEngine matchEngine)
	{
		super(set, name, description);
		this.smarts = smarts;
		this.matchEngine = matchEngine;
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

	@Override
	public SubstructureType getSubstructureType()
	{
		return set.getSubstructureType();
	}

	@Override
	public int getFrequency()
	{
		return frequency;
	}

	@Override
	public void setFrequency(int f)
	{
		this.frequency = f;
	}

	@Override
	public FragmentPropertySet getCompoundPropertySet()
	{
		return (FragmentPropertySet) set;
	}
}
