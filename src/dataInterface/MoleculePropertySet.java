package dataInterface;

import gui.binloc.Binary;
import data.DatasetFile;

public interface MoleculePropertySet
{
	public boolean isComputed(DatasetFile dataset);

	public boolean isCached(DatasetFile dataset);

	public boolean compute(DatasetFile dataset);

	public boolean isSizeDynamic();

	public boolean isSizeDynamicHigh(DatasetFile dataset);

	public int getSize(DatasetFile d);

	public MoleculeProperty get(DatasetFile d, int index);

	public String getDescription();

	public MoleculeProperty.Type getType();

	public Binary getBinary();

	public boolean isUsedForMapping();

	public String getNameIncludingParams();

	public boolean isComputationSlow();
}
