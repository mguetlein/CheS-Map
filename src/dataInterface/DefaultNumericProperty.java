package dataInterface;

import gui.property.ColorGradient;
import util.ArrayUtil;
import util.DoubleArraySummary;
import util.StringUtil;

public class DefaultNumericProperty extends AbstractCompoundProperty implements NumericProperty
{
	private boolean logEnabled = false;
	private ColorGradient colorGradient = null;
	private Double[] doubleValues;
	private Double[] normalizedValues;
	private Double[] normalizedLogValues;
	private Double median;
	private Integer distinct;
	private Boolean isInteger;
	private Boolean hasSmallDoubleValues;

	public DefaultNumericProperty(String name, String description, Double[] values)
	{
		super(name, description);
		setDoubleValues(values);
	}

	public DefaultNumericProperty(CompoundPropertySet set, String name, String description)
	{
		super(set, name, description);
	}

	@Override
	public boolean isValuesSet()
	{
		return doubleValues != null;
	}

	public void setDoubleValues(Double vals[])
	{
		vals = ArrayUtil.replaceNaN(vals, null);
		Double normalized[] = ArrayUtil.normalize(vals, false);
		setMissing(normalized);
		Double normalizedLog[] = ArrayUtil.normalizeLog(vals, false);
		doubleValues = vals;
		normalizedValues = normalized;
		normalizedLogValues = normalizedLog;
		DoubleArraySummary sum = DoubleArraySummary.create(normalized);
		median = sum.getMedian();
		distinct = sum.getNumDistinct();
		Boolean integerValue = true;
		for (Double d : vals)
			if (d != null && Math.round(d) != d)
			{
				integerValue = false;
				break;
			}
		isInteger = integerValue;
		Boolean smallDoubleValue = false;
		for (Double d : vals)
			if (d != null && d < 0.005 && d > -0.005)
			{
				smallDoubleValue = true;
				break;
			}
		hasSmallDoubleValues = smallDoubleValue;
	}

	@Override
	public String getFormattedValue(Double d)
	{
		if (d == null)
			return getFormattedNullValue();
		if (isInteger())
			return StringUtil.formatDouble(d, 0);
		else if (hasSmallDoubleValues())
			return StringUtil.formatDouble(d, 3);
		else
			return StringUtil.formatDouble(d);
	}

	@Override
	public int numDistinctValues()
	{
		return distinct;
	}

	@Override
	public Double[] getDoubleValues()
	{
		return doubleValues;
	}

	@Override
	public Double[] getNormalizedValues()
	{
		return normalizedValues;
	}

	@Override
	public Double[] getNormalizedLogValues()
	{
		return normalizedLogValues;
	}

	@Override
	public Double getNormalizedMedian()
	{
		return median;
	}

	@Override
	public Boolean isInteger()
	{
		return isInteger;
	}

	@Override
	public Boolean hasSmallDoubleValues()
	{
		return hasSmallDoubleValues;
	}

	@Override
	public boolean isLogHighlightingEnabled()
	{
		return logEnabled;
	}

	@Override
	public void setLogHighlightingEnabled(boolean log)
	{
		this.logEnabled = log;
	}

	@Override
	public ColorGradient getHighlightColorGradient()
	{
		return colorGradient;
	}

	@Override
	public void setHighlightColorGradient(ColorGradient colorGradient)
	{
		this.colorGradient = colorGradient;
	}

	@Override
	public boolean isUndefined()
	{
		return false;
	}
}
