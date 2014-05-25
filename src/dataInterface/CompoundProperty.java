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

	public static enum SubstructureType
	{
		MINE, MATCH
	}

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

	public String getUniqueName();

	public String[] getNominalDomain(DatasetFile dataset);

	public int[] getNominalDomainCounts(DatasetFile dataset);

	public String[] getNominalDomainInMappedDataset();

	public String[] getStringValues(DatasetFile dataset);

	public String getFormattedValueInMappedDataset(Object doubleOrString);

	public String getFormattedValue(Object doubleOrString, DatasetFile dataset);

	public String getFormattedNullValue();

	public Double[] getDoubleValues(DatasetFile dataset);

	public Double[] getDoubleValuesInCompleteMappedDataset();

	public Double[] getNormalizedValues(DatasetFile dataset);

	public Double[] getNormalizedValuesInCompleteMappedDataset();

	public Double[] getNormalizedLogValues(DatasetFile dataset);

	public Double getNormalizedMedian(DatasetFile dataset);

	public String getModeNonNull(DatasetFile dataset);

	public int numMissingValues(DatasetFile dataset);

	public int numMissingValuesInMappedDataset();

	/** excluding null */
	public int numDistinctValues(DatasetFile dataset);

	/** excluding null */
	public int numDistinctValuesInMappedDataset();

	public Boolean isInteger(DatasetFile dataset);

	public Boolean isIntegerInMappedDataset();

	public boolean isLogHighlightingEnabled();

	public void setLogHighlightingEnabled(boolean log);

	public ColorGradient getHighlightColorGradient();

	public void setHighlightColorGradient(ColorGradient grad);

	public Boolean hasSmallDoubleValues(DatasetFile dataset);

	public Boolean hasSmallDoubleValuesInMappedDataset();

	public CompoundProperty getRedundantProp(DatasetFile dataset);

	public CompoundProperty getRedundantPropInMappedDataset();

	public void setRedundantPropInMappedDataset(CompoundProperty b);

}
