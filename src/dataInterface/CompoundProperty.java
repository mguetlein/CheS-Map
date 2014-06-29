package dataInterface;

import gui.property.ColorGradient;

import java.awt.Color;

import data.fragments.MatchEngine;

public interface CompoundProperty
{
	public static enum Type
	{
		NUMERIC, NOMINAL
	}

	public static enum SubstructureType
	{
		MINE, MATCH
	}

	public boolean isValuesSet();

	public String getName();

	public String getDescription();

	public Type getType();

	public boolean isSmiles();

	public void setType(Type type);

	public boolean isTypeAllowed(Type type);

	public void setTypeAllowed(Type type, boolean allowed);

	public boolean isSmartsProperty();

	public String getSmarts();

	public MatchEngine getSmartsMatchEngine();

	public SubstructureType getSubstructureType();

	public CompoundPropertySet getCompoundPropertySet();

	//	public String getUniqueName();

	public String[] getNominalDomain();

	public int[] getNominalDomainCounts();

	public String[] getStringValuesInCompleteDataset();

	public String getFormattedValue(Object doubleOrString);

	public String getFormattedNullValue();

	public Double[] getDoubleValuesInCompleteDataset();

	public Double[] getNormalizedValuesInCompleteDataset();

	public Double[] getNormalizedLogValuesInCompleteDataset();

	public Double getNormalizedMedianInCompleteDataset();

	public String getModeNonNull();

	public int numMissingValuesInCompleteDataset();

	/** excluding null */
	public int numDistinctValuesInCompleteDataset();

	public Boolean isInteger();

	public boolean isLogHighlightingEnabled();

	public void setLogHighlightingEnabled(boolean log);

	public ColorGradient getHighlightColorGradient();

	public Color[] getHighlightColorSequence();

	public void setHighlightColorGradient(ColorGradient grad);

	public void setHighlightColorSequence(Color[] seq);

	public Boolean hasSmallDoubleValues();

	public CompoundProperty getRedundantProp();

	public void setRedundantProp(CompoundProperty b);

}
