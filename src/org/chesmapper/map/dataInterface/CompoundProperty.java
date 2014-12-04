package org.chesmapper.map.dataInterface;

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

	/**
	 * there are numeric or nominal properties
	 * numeric properties are never undefined
	 * non-integrated nominal properties are never undefined
	 * integrated nominal properties can be undefined, if they have a lot of different values (e.g. one for each compound)
	 * hence, they are not suited for mapping
	 * when selected in the viewer, undefined values should not be colored 
	 * the user can in the viewer make an undefined property to a defined nominal property (setting the type) 
	 */
	public boolean isUndefined();
}
