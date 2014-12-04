package org.chesmapper.map.dataInterface;

import java.awt.Color;

public interface NominalProperty extends CompoundProperty
{
	public String[] getDomain();

	public int[] getDomainCounts();

	public String[] getStringValues();

	public String getActiveValue();

	public String getFormattedValue(String value);

	public String getModeNonNull();

	public Color[] getHighlightColorSequence();

	public void setHighlightColorSequence(Color[] seq);

}
