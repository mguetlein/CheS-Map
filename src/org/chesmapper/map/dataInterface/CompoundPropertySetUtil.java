package org.chesmapper.map.dataInterface;

import java.util.List;

import org.mg.javalib.util.StringUtil;

public class CompoundPropertySetUtil
{
	public static CompoundPropertySet.Type getType(CompoundPropertySet[] set)
	{
		CompoundPropertySet.Type type = null;
		for (CompoundPropertySet s : set)
		{
			CompoundPropertySet.Type t = s.getType();
			if (t == null || (type != null && type != t))
				return null;
			else
				type = t;
		}
		return type;
	}

	public static CompoundPropertySet.Type getType(CompoundPropertySet set)
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
		//		Settings.LOGGER.warn("feature md5 key: " + name + " is based on " + list.size() + " feature sets");
		return StringUtil.getMD5(name);
	}

}
