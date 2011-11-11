package data.cdk;

import java.util.HashMap;

import util.ArrayUtil;
import dataInterface.AbstractMoleculeProperty;

public class CDKProperty extends AbstractMoleculeProperty
{
	private CDKDescriptor desc;
	private int index;

	private static HashMap<String, CDKProperty> instances = new HashMap<String, CDKProperty>();

	private CDKProperty(CDKDescriptor desc, int index)
	{
		super(getCDKPropertyName(desc, index), desc + " (CDK Descriptor)");

		this.desc = desc;
		this.index = index;

		if (ArrayUtil.indexOf(CDKDescriptor.CDK_NUMERIC_DESCRIPTORS, desc) != -1)
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

	private static String getCDKPropertyName(CDKDescriptor desc, int index)
	{
		return desc.getFeatureName(index);
	}

	public static CDKProperty fromString(String s, Type t)
	{
		CDKProperty p = CDKPropertySet.fromFeatureName(s);
		if (!p.isTypeAllowed(t))
			throw new IllegalArgumentException();
		p.setType(t);
		return p;
	}

	public static CDKProperty create(CDKDescriptor desc, int index)
	{
		if (!instances.containsKey(getCDKPropertyName(desc, index)))
			instances.put(getCDKPropertyName(desc, index), new CDKProperty(desc, index));
		return instances.get(getCDKPropertyName(desc, index));
	}

	@Override
	public CDKPropertySet getMoleculePropertySet()
	{
		return new CDKPropertySet(desc);
	}

	@Override
	public boolean equals(Object o)
	{
		return (o instanceof CDKProperty) && ((CDKProperty) o).desc.equals(desc) && ((CDKProperty) o).index == index;
	}

	public static void main(String args[])
	{
	}
}
