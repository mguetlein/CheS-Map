package org.chesmapper.map.dataInterface;

import javax.swing.ImageIcon;
import javax.vecmath.Vector3f;

public interface CompoundData extends SingleCompoundPropertyOwner
{
	public int getOrigIndex();

	public Vector3f getPosition();

	public String getSmiles();

	public ImageIcon getIcon(boolean blackBackground, int width, int height, boolean translucent);

	public Double getNormalizedValueCompleteDataset(NumericProperty p);
}
