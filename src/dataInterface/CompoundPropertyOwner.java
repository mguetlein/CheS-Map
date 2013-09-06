package dataInterface;

public interface CompoundPropertyOwner
{
	/**
	 * only supported for numeric props, may be null, mean for clusters
	 * 
	 * @param p
	 * @return
	 */
	public Double getDoubleValue(CompoundProperty p);

	/**
	 * only supported for nominal props, string value, most-frequent value for clusters
	 * 
	 * @param p
	 * @return
	 */
	public String getStringValue(CompoundProperty p);

	public String getFormattedValue(CompoundProperty p);
}
