package dataInterface;

public interface CompoundProperty
{
	public boolean isValuesSet();

	public String getName();

	public String getDescription();

	public CompoundPropertySet getCompoundPropertySet();

	public String getFormattedNullValue();

	public int numMissingValues();

	/** excluding null */
	public int numDistinctValues();

	public CompoundProperty getRedundantProp();

	public void setRedundantProp(CompoundProperty b);

}
