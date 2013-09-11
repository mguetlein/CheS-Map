package dataInterface;

import gui.property.ColorGradient;
import data.DatasetFile;
import data.fragments.MatchEngine;

public interface CompoundProperty
{
	public static enum Type
	{
		NUMERIC, NOMINAL
	}

	public String getName();

	public String getDescription();

	public Type getType();

	public boolean isSmiles();

	public void setType(Type type);

	public boolean isTypeAllowed(Type type);

	public void setTypeAllowed(Type type, boolean allowed);

	public String[] getNominalDomain();

	public boolean isSmartsProperty();

	public String getSmarts();

	public MatchEngine getSmartsMatchEngine();

	public CompoundPropertySet getCompoundPropertySet();

	public String getUniqueName();

	public String[] getStringValues(DatasetFile dataset);

	public Double[] getDoubleValues(DatasetFile dataset);

	public Double[] getNormalizedValues(DatasetFile dataset);

	public Double[] getNormalizedLogValues(DatasetFile dataset);

	public Double getNormalizedMedian(DatasetFile dataset);

	public String getModeNonNull(DatasetFile dataset);

	public int numMissingValues(DatasetFile dataset);

	public int numDistinctValues(DatasetFile dataset);

	public Boolean isInteger(DatasetFile dataset);

	public void setMappedDataset(DatasetFile dataset);

	public Boolean isIntegerInMappedDataset();

	public boolean isLogHighlightingEnabled();

	public void setLogHighlightingEnabled(boolean log);

	public ColorGradient getHighlightColorGradient();

	public void setHighlightColorGradient(ColorGradient grad);

	public Boolean hasSmallDoubleValues(DatasetFile dataset);

	public Boolean hasSmallDoubleValuesInMappedDataset();

}
