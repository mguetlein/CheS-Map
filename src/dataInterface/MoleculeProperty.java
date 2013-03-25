package dataInterface;

import data.DatasetFile;
import data.fragments.MatchEngine;

public interface MoleculeProperty
{
	public static enum Type
	{
		NUMERIC, NOMINAL
	}

	public String getName();

	public String getDescription();

	public Type getType();

	public void setType(Type type);

	public boolean isTypeAllowed(Type type);

	public void setTypeAllowed(Type type, boolean allowed);

	public String[] getNominalDomain();

	public boolean isSmartsProperty();

	public String getSmarts();

	public MatchEngine getSmartsMatchEngine();

	public MoleculePropertySet getMoleculePropertySet();

	public String[] getStringValues(DatasetFile dataset);

	public Double[] getDoubleValues(DatasetFile dataset);

	public Double[] getNormalizedValues(DatasetFile dataset);

	public Double[] getNormalizedLogValues(DatasetFile dataset);

	public Double getNormalizedMedian(DatasetFile dataset);

	public String getUniqueName();

	public int numMissingValues(DatasetFile dataset);

	public int numDistinctValues(DatasetFile dataset);

}
