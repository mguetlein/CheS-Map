package alg.embed3d;

import gui.binloc.Binary;

import java.util.List;

import javax.vecmath.Vector3f;

import util.Vector3fUtil;
import data.DatasetFile;
import dataInterface.AbstractCompoundProperty;
import dataInterface.CompoundProperty;
import dataInterface.CompoundPropertySet;

public class DistanceToProperty extends AbstractCompoundProperty implements CompoundPropertySet
{

	public DistanceToProperty(DatasetFile dataset, String compoundName, int index, List<Vector3f> positions)
	{
		super("Distance to " + compoundName, "");
		Double s[] = new Double[positions.size()];
		for (int i = 0; i < s.length; i++)
			s[i] = (double) Vector3fUtil.dist(positions.get(index), positions.get(i));
		setDoubleValues(dataset, s);
	}

	@Override
	public CompoundPropertySet getCompoundPropertySet()
	{
		return this;
	}

	@Override
	public boolean compute(DatasetFile dataset)
	{
		return true;
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
	public CompoundProperty get(DatasetFile d, int index)
	{
		return this;
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
		return toString();
	}

	@Override
	public boolean isComputationSlow()
	{
		return false;
	}

	@Override
	public Type getType()
	{
		return Type.NUMERIC;
	}

	@Override
	public boolean isSensitiveTo3D()
	{
		return false;
	}
}
