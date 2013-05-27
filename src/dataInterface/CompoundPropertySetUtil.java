package dataInterface;

import java.util.List;

import util.StringUtil;
import dataInterface.CompoundProperty.Type;

public class CompoundPropertySetUtil
{
	public static Type getType(CompoundPropertySet[] set)
	{
		Type type = null;
		for (CompoundPropertySet s : set)
		{
			CompoundProperty.Type t = s.getType();
			if (t == null || (type != null && type != t))
				return null;
			else
				type = t;
		}
		return type;
	}

	public static Type getType(CompoundPropertySet set)
	{
		return getType(new CompoundPropertySet[] { set });
	}

	public static String getMD5(List<CompoundPropertySet> list)
	{
		return getMD5(list, "");
	}

	public static String getMD5(List<CompoundPropertySet> list, String additionalParam)
	{
		String name = additionalParam;
		for (CompoundPropertySet p : list)
			name += p.getNameIncludingParams();
		//		Settings.LOGGER.warn("feature md5 key: " + name);
		return StringUtil.getMD5(name);
	}

}
