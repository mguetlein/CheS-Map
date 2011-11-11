package data.fragments;

import dataInterface.AbstractFragmentProperty;
import dataInterface.MoleculePropertySet;

public class StructuralFragment extends AbstractFragmentProperty
{
	StructuralFragmentSet set;

	public StructuralFragment(String name, MatchEngine matchEngine, String smarts)
	{
		super(name, name + "_" + matchEngine, "Structural Fragment, matched with " + matchEngine, smarts);
	}

	@Override
	public MoleculePropertySet getMoleculePropertySet()
	{
		return set;
	}
}