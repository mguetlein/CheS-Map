package dataInterface;

public interface MolecularPropertyOwner
{
	public Double getValue(MoleculeProperty p, boolean normalized);

	public Object getObjectValue(MoleculeProperty p, boolean normalized);
}
