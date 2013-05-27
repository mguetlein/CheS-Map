package dataInterface;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import util.ArrayUtil;
import util.DoubleArraySummary;
import util.ObjectUtil;
import data.DatasetFile;
import dataInterface.CompoundProperty.Type;
import freechart.FreeChartUtil;

public class CompoundPropertyUtil
{
	public static int computeNumDistinct(Double values[])
	{
		return DoubleArraySummary.create(values).getNum();
	}

	public static int computeNumDistinct(String values[])
	{
		Set<String> distinctValues = ArrayUtil.getDistinctValues(values);
		if (distinctValues.contains(null))
			distinctValues.remove(null);
		return distinctValues.size();
	}

	public static List<String[]> valuesReplaceNullWithMedian(List<CompoundProperty> props,
			List<CompoundPropertyOwner> values, DatasetFile dataset)
	{
		List<String[]> v = new ArrayList<String[]>();
		for (CompoundPropertyOwner vv : values)
		{
			String d[] = new String[props.size()];
			int count = 0;
			for (CompoundProperty p : props)
			{
				if (p.getType() == Type.NUMERIC)
				{
					Double val = vv.getNormalizedValue(p);
					if (val == null)
						d[count++] = p.getNormalizedMedian(dataset) + "";
					else
						d[count++] = vv.getNormalizedValue(p) + "";
				}
				else
					d[count++] = vv.getStringValue(p);
			}
			v.add(d);
		}
		return v;
	}

	private static HashMap<CompoundProperty, Color[]> mapping = new HashMap<CompoundProperty, Color[]>();

	private static Color[] AVAILABLE_COLORS = FreeChartUtil.BRIGHT_COLORS;
	static
	{
		Color col = AVAILABLE_COLORS[0];
		AVAILABLE_COLORS[0] = AVAILABLE_COLORS[1];
		AVAILABLE_COLORS[1] = col;
	}

	public static Color getColor(int index)
	{
		//		if (index + 1 > AVAILABLE_COLORS.length)
		//			return Color.GRAY;
		//		else
		return AVAILABLE_COLORS[index % AVAILABLE_COLORS.length];
	}

	public static Color[] getNominalColors(CompoundProperty p)
	{
		if (p.getType() != Type.NOMINAL)
			throw new IllegalArgumentException();

		if (!mapping.containsKey(p))
		{
			Color col[];
			if (p.getNominalDomain().length > AVAILABLE_COLORS.length)
			{
				Color gray[] = new Color[p.getNominalDomain().length - AVAILABLE_COLORS.length];
				Arrays.fill(gray, Color.GRAY);
				col = ArrayUtil.concat(AVAILABLE_COLORS, gray);
			}
			else
				col = Arrays.copyOfRange(AVAILABLE_COLORS, 0, p.getNominalDomain().length);
			mapping.put(p, col);
		}
		return mapping.get(p);
	}

	public static Color getNominalColor(CompoundProperty p, String val)
	{
		int index = ArrayUtil.indexOf(p.getNominalDomain(), val);
		if (index == -1)
			throw new IllegalStateException(val + " not found in " + ArrayUtil.toString(p.getNominalDomain()));
		return getNominalColors(p)[index];
	}

	public static boolean containsSmartsProperty(List<CompoundProperty> list)
	{
		for (CompoundProperty p : list)
			if (p.isSmartsProperty())
				return true;
		return false;
	}

	public static String getSetMD5(List<CompoundProperty> list)
	{
		return getSetMD5(list, "");
	}

	public static String getSetMD5(List<CompoundProperty> list, String additionalParam)
	{
		return CompoundPropertySetUtil.getMD5(getSets(list), additionalParam);
	}

	public static List<CompoundPropertySet> getSets(List<CompoundProperty> list)
	{
		List<CompoundPropertySet> sets = new ArrayList<CompoundPropertySet>();
		for (CompoundProperty p : list)
			if (sets.indexOf(p.getCompoundPropertySet()) == -1)
				sets.add(p.getCompoundPropertySet());
		return sets;
	}

	public static boolean hasUniqueValues(List<CompoundProperty> list, DatasetFile data)
	{
		for (CompoundProperty p : list)
			if (!hasUniqueValue(p, data))
				return false;
		return true;
	}

	public static boolean hasUniqueValue(CompoundProperty prop, DatasetFile data)
	{
		boolean first = true;
		Object value = null;
		for (Object val : (prop.getType() == Type.NUMERIC ? prop.getDoubleValues(data) : prop.getStringValues(data)))
		{
			if (first)
			{
				value = val;
				first = false;
			}
			else
			{
				if (!ObjectUtil.equals(value, val))
					return false;
			}
		}
		return true;
	}

}
