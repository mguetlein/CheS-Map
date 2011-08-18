package dataInterface;

public interface MolecularPropertyOwner
{
	/**
	 * only supported for numeric props, may be null, mean for clusters
	 * 
	 * @param p
	 * @return
	 */
	public Double getDoubleValue(MoleculeProperty p);

	/**
	 * only supported for nominal props, string value, most-frequent value for clusters
	 * 
	 * @param p
	 * @return
	 */
	public String getStringValue(MoleculeProperty p);

	/**
	 * supported for non-numeric structures as well 
	 * 
	 * @param p
	 * @return
	 */
	public double getNormalizedValue(MoleculeProperty p);

}
