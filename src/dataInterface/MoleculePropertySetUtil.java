package dataInterface;

import dataInterface.MoleculeProperty.Type;

public class MoleculePropertySetUtil
{
	public static Type getType(MoleculePropertySet set)
	{
		Type type;
		if (set.getSize() == 1)
		{
			type = set.get(0).getType();
		}
		else
		{
			type = null;
			for (int i = 0; i < set.getSize(); i++)
			{
				MoleculeProperty.Type t = set.get(i).getType();
				if (t == null || (type != null && type != t))
				{
					type = null;
					break;
				}
				else
					type = t;
			}
		}
		return type;
	}

}
