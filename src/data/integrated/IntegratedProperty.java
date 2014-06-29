package data.integrated;

import dataInterface.DefaultCompoundProperty;

public class IntegratedProperty extends DefaultCompoundProperty
{

	public IntegratedProperty(String property)
	{
		super(property, /*property + "." + dataset.toString() + "." + dataset.getMD5(),*/"Included in Dataset");
	}
}
