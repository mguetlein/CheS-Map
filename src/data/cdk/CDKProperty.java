package data.cdk;

import util.ArrayUtil;
import dataInterface.DefaultCompoundProperty;

public class CDKProperty extends DefaultCompoundProperty
{
	private CDKDescriptor desc;
	private int index;

	public CDKProperty(CDKDescriptor desc, int index)
	{
		super(desc.getFeatureName(index), desc + " (CDK Descriptor)");

		this.desc = desc;
		this.index = index;

		if (ArrayUtil.indexOf(CDKDescriptor.getNumericDescriptors(), desc) != -1)
		{
			setTypeAllowed(Type.NOMINAL, false);
			setType(Type.NUMERIC);
		}
		else
		{
			setTypeAllowed(Type.NUMERIC, false);
			setType(Type.NOMINAL);
		}
	}

	@Override
	public CDKPropertySet getCompoundPropertySet()
	{
		return new CDKPropertySet(desc);
	}

	@Override
	public boolean equals(Object o)
	{
		return (o instanceof CDKProperty) && ((CDKProperty) o).desc.equals(desc) && ((CDKProperty) o).index == index;
	}
}
