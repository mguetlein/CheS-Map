package org.chesmapper.map.dataInterface;

import org.chesmapper.map.dataInterface.CompoundPropertyOwner;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.mg.javalib.util.CountedSet;

public interface CompoundGroupWithProperties extends CompoundPropertyOwner
{
	public int getNumCompounds();

	public CountedSet<String> getNominalSummary(NominalProperty p);
}
