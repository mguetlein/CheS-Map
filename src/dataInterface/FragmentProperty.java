package dataInterface;

import data.fragments.MatchEngine;

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
