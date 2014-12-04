package org.chesmapper.map.dataInterface;

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
	 * referes to mapped dataset
	 * @return
	 */
	public String getFormattedValue(CompoundProperty p);
}
