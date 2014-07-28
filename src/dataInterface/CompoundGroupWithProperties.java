package dataInterface;

import util.CountedSet;
import dataInterface.CompoundPropertyOwner;
import dataInterface.NominalProperty;

public interface CompoundGroupWithProperties extends CompoundPropertyOwner
{
	public int getNumCompounds();

	public CountedSet<String> getNominalSummary(NominalProperty p);
}
