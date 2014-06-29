package data.fragments;

import dataInterface.DefaultFragmentProperty;

public class ListedFragmentProperty extends DefaultFragmentProperty
{
	int minNumMatches;

	public ListedFragmentProperty(String name, String description, String smarts, MatchEngine matchEngine,
			int minNumMatches)
	{
		super(null, name, description, smarts, matchEngine);
		this.minNumMatches = minNumMatches;
	}

	public void setListedFragmentSet(ListedFragmentSet set)
	{
		this.set = set;
	}

	public int getMinNumMatches()
	{
		return minNumMatches;
	}
}