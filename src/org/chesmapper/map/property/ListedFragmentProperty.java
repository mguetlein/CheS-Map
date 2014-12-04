package org.chesmapper.map.property;

import org.chesmapper.map.data.fragments.MatchEngine;
import org.chesmapper.map.dataInterface.DefaultFragmentProperty;

public class ListedFragmentProperty extends DefaultFragmentProperty
{
	int minNumMatches;

	ListedFragmentProperty(String name, String description, String smarts, MatchEngine matchEngine, int minNumMatches)
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