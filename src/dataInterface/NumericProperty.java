package dataInterface;

import gui.property.ColorGradient;

public interface NumericProperty extends CompoundProperty
{
	public String getFormattedValue(Double value);

	public Double[] getDoubleValues();

	public Double[] getNormalizedValues();

	public Double getNormalizedMedian();

	public Boolean isInteger();

	public ColorGradient getHighlightColorGradient();

	public Boolean hasSmallDoubleValues();

	public void setHighlightColorGradient(ColorGradient grad);

}
