package alg.embed3d;

import gui.binloc.Binary;
import util.ArrayUtil;
import data.DatasetFile;
import dataInterface.AbstractMoleculeProperty;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculePropertySet;

public class CCCMoleculePropertySet extends AbstractMoleculeProperty implements MoleculePropertySet
{
	public CCCMoleculePropertySet(DatasetFile data, double d[])
	{
		super("ccc", "ccc", "ccc description");
		setDoubleValues(data, ArrayUtil.toDoubleArray(d));
	}

	@Override
	public MoleculePropertySet getMoleculePropertySet()
	{
		return this;
	}

	@Override
	public boolean isComputed(DatasetFile dataset)
	{
		return true;
	}

	@Override
	public boolean isCached(DatasetFile dataset)
	{
		return true;
	}

	@Override
	public boolean compute(DatasetFile dataset)
	{
		return true;
	}

	@Override
	public boolean isSizeDynamic()
	{
		return false;
	}

	@Override
	public boolean isSizeDynamicHigh(DatasetFile dataset)
	{
		return false;
	}

	@Override
	public int getSize(DatasetFile d)
	{
		return 1;
	}

	@Override
	public MoleculeProperty get(DatasetFile d, int index)
	{
		return this;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public Type getType()
	{
		return Type.NUMERIC;
	}

	@Override
	public Binary getBinary()
	{
		return null;
	}

	@Override
	public boolean isUsedForMapping()
	{
		return false;
	}

	@Override
	public String getNameIncludingParams()
	{
		return name;
	}

	@Override
	public boolean isComputationSlow()
	{
		return false;
	}

}
