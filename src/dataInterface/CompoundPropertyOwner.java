package dataInterface;

public interface CompoundPropertyOwner
{
	/**
	 * only supported for numeric props, may be null, mean for clusters
	 * 
	 * @param p
	 * @return
	 */
	public Double getDoubleValue(NumericProperty p);

	/**
	 * only supported for nominal props, string value, most-frequent value for clusters
	 * 
	 * @param p
	 * @return
	 */
	public String getStringValue(NominalProperty p);

	/**
	 * referes to mapped dataset
	 * @return
	 */
	public String getFormattedValue(CompoundProperty p);
}
