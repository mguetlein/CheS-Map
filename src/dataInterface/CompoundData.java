package dataInterface;

import javax.vecmath.Vector3f;

public interface CompoundData extends MolecularPropertyOwner
{
	public int getIndex();

	public Vector3f getPosition();

	public String getSmiles();
}
