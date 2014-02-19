package dataInterface;

import util.ArrayUtil;
import util.DoubleArraySummary;
import data.DatasetFile;

public abstract class NumericDynamicCompoundProperty extends DynamicCompoundProperty
{
	Double[] doubleValues;
	Double[] normalizedValues;
	Double[] normalizedLogValues;
	Double median;
	Integer missing;
	Integer distinct;
	Boolean isInteger;
	Boolean hasSmallDoubleValues;

	public NumericDynamicCompoundProperty(Double vals[])
	{
		vals = ArrayUtil.replaceNaN(vals, null);
		doubleValues = vals;
		normalizedValues = ArrayUtil.normalize(vals, false);
		normalizedLogValues = ArrayUtil.normalizeLog(vals, false);

		DoubleArraySummary sum = DoubleArraySummary.create(normalizedValues);
		missing = sum.getNullCount();
		median = sum.getMedian();
		distinct = sum.getNumDistinct();
		isInteger = true;
		for (Double d : vals)
			if (d != null && Math.round(d) != d)
			{
				isInteger = false;
				break;
			}
		hasSmallDoubleValues = false;
		for (Double d : vals)
			if (d != null && d < 0.005 && d > -0.005)
			{
				hasSmallDoubleValues = true;
				break;
			}
	}

	@Override
	public String getFormattedValueInMappedDataset(Object doubleOrString)
	{
		return AbstractCompoundProperty.getFormattedValueInMappedDataset(this, doubleOrString);
	}

	@Override
	public String getFormattedValue(Object doubleOrString, DatasetFile dataset)
	{
		return AbstractCompoundProperty.getFormattedValue(this, doubleOrString, dataset);
	}

	@Override
	public String toString()
	{
		return getName();
	}

	@Override
	public Type getType()
	{
		return Type.NUMERIC;
	}

	@Override
	public String[] getNominalDomainInMappedDataset()
	{
		return null;
	}

	@Override
	public Boolean isIntegerInMappedDataset()
	{
		return isInteger;
	}

	@Override
	public Boolean hasSmallDoubleValuesInMappedDataset()
	{
		return hasSmallDoubleValues;
	}

	@Override
	public Double[] getNormalizedValuesInCompleteMappedDataset()
	{
		return normalizedValues;
	}

	@Override
	public Double[] getDoubleValuesInCompleteMappedDataset()
	{
		return doubleValues;
	}

}
