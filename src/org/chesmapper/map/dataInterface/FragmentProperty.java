package org.chesmapper.map.dataInterface;

import org.chesmapper.map.data.fragments.MatchEngine;

public interface FragmentProperty extends NominalProperty
{
	public static enum SubstructureType
	{
		MINE, MATCH
	}

	public String getSmarts();

	public MatchEngine getSmartsMatchEngine();

	public SubstructureType getSubstructureType();

	public void setFrequency(int f);

	public int getFrequency();

}
