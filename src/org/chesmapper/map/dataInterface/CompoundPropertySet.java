package org.chesmapper.map.dataInterface;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.FragmentProperty.SubstructureType;
import org.mg.javalib.gui.binloc.Binary;

public interface CompoundPropertySet
{
	public static enum Type
	{
		NUMERIC, NOMINAL
	}

	public boolean isComputed(DatasetFile dataset);

	public boolean isCached(DatasetFile dataset);

	public boolean compute(DatasetFile dataset);

	public boolean isSizeDynamic();

	public boolean isSizeDynamicHigh(DatasetFile dataset);

	public int getSize(DatasetFile d);

	public CompoundProperty get(DatasetFile d, int index);

	public void clearComputedProperties(DatasetFile d);

	public String getDescription();

	public String serialize();

	public void setType(Type type);

	public Type getType();

	public boolean isTypeAllowed(Type type);

	public void setTypeAllowed(Type type, boolean allowed);

	public Binary getBinary();

	public boolean isSelectedForMapping();

	public String getNameIncludingParams();

	public boolean isComputationSlow();

	public boolean isSensitiveTo3D();

	public SubstructureType getSubstructureType();

	public void setSmiles(boolean smiles);

	public boolean isSmiles();

	public boolean isHiddenFromGUI();

}
