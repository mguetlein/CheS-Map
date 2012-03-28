package data.fragments;

import dataInterface.AbstractFragmentProperty;
import dataInterface.MoleculePropertySet;

public class StructuralFragment extends AbstractFragmentProperty
{
	StructuralFragmentSet set;

	public StructuralFragment(String name, MatchEngine matchEngine, String file, String smarts)
	{
		super(name, name + "_" + matchEngine + "_" + file, "Structural Fragment, matched with " + matchEngine, smarts,
				matchEngine);
	}

	@Override
	public MoleculePropertySet getMoleculePropertySet()
	{
		return set;
	}
}