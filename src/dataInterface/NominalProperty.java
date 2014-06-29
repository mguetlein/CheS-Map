package dataInterface;

import java.awt.Color;

public interface NominalProperty extends CompoundProperty
{
	public void setSmiles(boolean smiles);

	public boolean isSmiles();

	public String[] getDomain();

	public int[] getDomainCounts();

	public String[] getStringValues();

	public String getFormattedValue(String value);

	public String getModeNonNull();

	public Color[] getHighlightColorSequence();

	public void setHighlightColorSequence(Color[] seq);

}
