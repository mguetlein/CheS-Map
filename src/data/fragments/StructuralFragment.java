package data.fragments;

import dataInterface.FragmentProperty;

public class StructuralFragment extends FragmentProperty
{
	//	StructuralFragmentSet set;
	int minNumMatches;

	//	public StructuralFragment(String name, MatchEngine matchEngine, String file, String smarts, int minNumMatches)
	//	{
	//		super(name, //name + "_" + matchEngine + "_" + file, 
	//				"Structural Fragment, matched with " + matchEngine, smarts, matchEngine);
	//		this.minNumMatches = minNumMatches;
	//	}
	//
	//	@Override
	//	public CompoundPropertySet getCompoundPropertySet()
	//	{
	//		return set;
	//	}

	public StructuralFragment(String name, String description, String smarts, MatchEngine matchEngine, int minNumMatches)
	{
		super(null, name, description, smarts, matchEngine);
		this.minNumMatches = minNumMatches;
	}

	public void setStructuralFragmentSet(StructuralFragmentSet set)
	{
		this.set = set;
	}

	public int getMinNumMatches()
	{
		return minNumMatches;
	}
}