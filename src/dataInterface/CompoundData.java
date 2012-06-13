package dataInterface;

import javax.swing.ImageIcon;
import javax.vecmath.Vector3f;

public interface CompoundData extends MolecularPropertyOwner
{
	public int getIndex();

	public Vector3f getPosition();

	public String getSmiles();

	public ImageIcon getIcon(boolean blackBackground);
}
