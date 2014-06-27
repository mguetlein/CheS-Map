package dataInterface;

import gui.binloc.Binary;
import gui.property.ColorGradient;

import java.awt.Color;
import java.nio.channels.IllegalSelectorException;

import data.DatasetFile;
import data.fragments.MatchEngine;

public abstract class DynamicCompoundProperty implements CompoundProperty, CompoundPropertySet
{
	@Override
	public String getFormattedNullValue()
	{
		return "missing";
	}

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
	public String[] getStringValuesInCompleteMappedDataset()
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
	public Double getNormalizedMedianInCompleteMappedDataset()
	{
		throw new IllegalSelectorException();
	}

	@Override
	public String getModeNonNull(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

	@Override
	public String getModeNonNullInMappedDataset()
	{
		throw new IllegalStateException();
	}

	@Override
	public int numMissingValues(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}

	@Override
	public int numMissingValuesInMappedDataset()
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

	ColorGradient colorGradient;
	Color[] colorSequence;

	@Override
	public ColorGradient getHighlightColorGradient()
	{
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		return colorGradient;
	}

	@Override
	public void setHighlightColorGradient(ColorGradient colorGradient)
	{
		if (getType() != Type.NUMERIC)
			throw new IllegalStateException();
		this.colorGradient = colorGradient;
	}

	@Override
	public Color[] getHighlightColorSequence()
	{
		if (getType() == Type.NUMERIC)
			throw new IllegalStateException();
		return colorSequence;
	}

	@Override
	public void setHighlightColorSequence(Color[] seq)
	{
		if (getType() == Type.NUMERIC)
			throw new IllegalStateException();
		colorSequence = seq;
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
	public boolean isSelectedForMapping()
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

	@Override
	public CompoundProperty getRedundantProp(DatasetFile dataset)
	{
		return null;
	}

	@Override
	public CompoundProperty getRedundantPropInMappedDataset()
	{
		return null;
	}

	@Override
	public void setRedundantPropInMappedDataset(CompoundProperty b)
	{
		throw new IllegalStateException();
	}

	@Override
	public Double[] getDoubleValuesInCompleteMappedDataset()
	{
		throw new IllegalStateException();
	}

	@Override
	public Double[] getNormalizedValuesInCompleteMappedDataset()
	{
		throw new IllegalStateException();
	}

	@Override
	public Boolean isIntegerInMappedDataset()
	{
		throw new IllegalStateException();
	}

	@Override
	public Boolean hasSmallDoubleValuesInMappedDataset()
	{
		throw new IllegalStateException();
	}

}
