package dataInterface;

import data.DatasetFile;

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

	public Object[] getNominalDomain();

	public void setNominalDomain(Object domain[]);

	public boolean isSmartsProperty();

	public String getSmarts();

	public MoleculePropertySet getMoleculePropertySet();

	public String[] getStringValues(DatasetFile dataset);

	public Double[] getDoubleValues(DatasetFile dataset);

	public Double[] getNormalizedValues(DatasetFile dataset);
}
