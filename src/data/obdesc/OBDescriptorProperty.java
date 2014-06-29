package data.obdesc;

import dataInterface.DefaultCompoundProperty;

public class OBDescriptorProperty extends DefaultCompoundProperty
{

	public OBDescriptorProperty(String name, String description, Type defaultType)
	{
		super(name, description);

		if (defaultType == Type.NUMERIC)
		{
			setTypeAllowed(Type.NUMERIC, true);
			setTypeAllowed(Type.NOMINAL, false);
			setType(Type.NUMERIC);
		}
		else if (defaultType == Type.NOMINAL)
		{
			setTypeAllowed(Type.NUMERIC, false);
			setTypeAllowed(Type.NOMINAL, true);
			setType(Type.NOMINAL);
		}
		else
		{
			setTypeAllowed(Type.NUMERIC, true);
			setTypeAllowed(Type.NOMINAL, true);
		}
	}

}
