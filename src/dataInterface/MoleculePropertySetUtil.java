package dataInterface;

import java.util.List;

import util.StringUtil;
import dataInterface.MoleculeProperty.Type;

public class MoleculePropertySetUtil
{
	public static Type getType(MoleculePropertySet[] set)
	{
		Type type = null;
		for (MoleculePropertySet s : set)
		{
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

	public static String getMD5(List<MoleculePropertySet> list)
	{
		return getMD5(list, "");
	}

	public static String getMD5(List<MoleculePropertySet> list, String additionalParam)
	{
		String name = additionalParam;
		for (MoleculePropertySet p : list)
			name += p.getNameIncludingParams();
		//		System.err.println("feature md5 key: " + name);
		return StringUtil.getMD5(name);
	}

}
