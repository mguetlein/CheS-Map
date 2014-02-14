package dataInterface;

import gui.binloc.Binary;
import gui.property.ColorGradient;
import data.DatasetFile;
import data.fragments.MatchEngine;

public abstract class DynamicCompoundProperty implements CompoundProperty, CompoundPropertySet
{
	@Override
	public boolean isSmiles()
	{
		return false;
	}

	@Override
	public boolean isTypeAllowed(Type type)
	{
		throw new IllegalStateException();
	}

	@Override
	public void setTypeAllowed(Type type, boolean allowed)
	{
		throw new IllegalStateException();
	}

	@Override
	public boolean isSmartsProperty()
	{
		return false;
	}

	@Override
	public String getSmarts()
	{
		return null;
	}

	@Override
	public MatchEngine getSmartsMatchEngine()
	{
		throw new IllegalStateException();
	}

	@Override
	public SubstructureType getSubstructureType()
	{
		throw new IllegalStateException();
	}

	@Override
	public CompoundPropertySet getCompoundPropertySet()
	{
		return this;
	}

	@Override
	public String getUniqueName()
	{
		throw new IllegalStateException();
	}

	@Override
	public String[] getNominalDomain(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

	@Override
	public int[] getNominalDomainCounts(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

	@Override
	public String[] getStringValues(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

	@Override
	public Double[] getDoubleValues(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

	@Override
	public Double[] getNormalizedValues(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

	@Override
	public Double[] getNormalizedLogValues(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

	@Override
	public Double getNormalizedMedian(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

	@Override
	public String getModeNonNull(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

	@Override
	public int numMissingValues(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

	@Override
	public int numDistinctValues(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

	@Override
	public Boolean isInteger(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

	boolean log;

	@Override
	public boolean isLogHighlightingEnabled()
	{
		return log;
	}

	@Override
	public void setLogHighlightingEnabled(boolean log)
	{
		this.log = log;
	}

	ColorGradient grad;

	@Override
	public ColorGradient getHighlightColorGradient()
	{
		return grad;
	}

	@Override
	public void setHighlightColorGradient(ColorGradient grad)
	{
		this.grad = grad;
	}

	@Override
	public boolean isComputed(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

	@Override
	public boolean isCached(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

	@Override
	public boolean compute(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

	@Override
	public boolean isSizeDynamic()
	{
		return false;
	}

	@Override
	public boolean isSizeDynamicHigh(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

	@Override
	public int getSize(DatasetFile d)
	{
		return 1;
	}

	@Override
	public CompoundProperty get(DatasetFile d, int index)
	{
		throw new IllegalStateException();
	}

	@Override
	public Binary getBinary()
	{
		throw new IllegalStateException();
	}

	@Override
	public boolean isUsedForMapping()
	{
		return false;
	}

	@Override
	public String getNameIncludingParams()
	{
		throw new IllegalStateException();
	}

	@Override
	public boolean isComputationSlow()
	{
		throw new IllegalStateException();
	}

	@Override
	public boolean isSensitiveTo3D()
	{
		throw new IllegalStateException();
	}

	@Override
	public void setType(Type type)
	{
		throw new IllegalStateException();
	}

	@Override
	public Boolean hasSmallDoubleValues(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

}
