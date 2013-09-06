package data.fragments;

import dataInterface.AbstractFragmentProperty;
import dataInterface.CompoundPropertySet;

public class StructuralFragment extends AbstractFragmentProperty
{
	StructuralFragmentSet set;
	int minNumMatches;

	public StructuralFragment(String name, MatchEngine matchEngine, String file, String smarts, int minNumMatches)
	{
		super(name, name + "_" + matchEngine + "_" + file, "Structural Fragment, matched with " + matchEngine, smarts,
				matchEngine);
		this.minNumMatches = minNumMatches;
	}

	@Override
	public CompoundPropertySet getCompoundPropertySet()
	{
		return set;
	}

	public int getMinNumMatches()
	{
		return minNumMatches;
	}
}