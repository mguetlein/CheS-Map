package dataInterface;

import dataInterface.MoleculeProperty.Type;

public class MoleculePropertySetUtil
{
	public static Type getType(MoleculePropertySet[] set)
	{
		Type type = null;
		for (MoleculePropertySet s : set)
		{
			//			for (int i = 0; i < s.getSize(); i++)
			//			{
			//				MoleculeProperty.Type t = s.get(i).getType();
			//				if (t == null || (type != null && type != t))
			//					return null;
			//				else
			//					type = t;
			//			}
			MoleculeProperty.Type t = s.getType();
			if (t == null || (type != null && type != t))
				return null;
			else
				type = t;
		}
		return type;
	}

	public static Type getType(MoleculePropertySet set)
	{
		return getType(new MoleculePropertySet[] { set });
	}

}
