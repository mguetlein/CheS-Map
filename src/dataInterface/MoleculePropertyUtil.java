package dataInterface;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import util.ArrayUtil;
import util.ObjectUtil;
import data.DatasetFile;
import dataInterface.MoleculeProperty.Type;
import freechart.FreeChartUtil;

public class MoleculePropertyUtil
{
	private static HashMap<MoleculeProperty, Color[]> mapping = new HashMap<MoleculeProperty, Color[]>();

	private static Color[] AVAILABLE_COLORS = FreeChartUtil.BRIGHT_COLORS;
	static
	{
		Color col = AVAILABLE_COLORS[0];
		AVAILABLE_COLORS[0] = AVAILABLE_COLORS[1];
		AVAILABLE_COLORS[1] = col;
	}

	public static Color getColor(int index)
	{
		if (index + 1 > AVAILABLE_COLORS.length)
			return Color.GRAY;
		else
			return AVAILABLE_COLORS[index];
	}

	public static Color[] getNominalColors(MoleculeProperty p)
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

	public static Color getNominalColor(MoleculeProperty p, String val)
	{
		int index = ArrayUtil.indexOf(p.getNominalDomain(), val);
		if (index == -1)
			throw new IllegalStateException(val + " not found in " + ArrayUtil.toString(p.getNominalDomain()));
		return getNominalColors(p)[index];
	}

	public static boolean containsSmartsProperty(List<MoleculeProperty> list)
	{
		for (MoleculeProperty p : list)
			if (p.isSmartsProperty())
				return true;
		return false;
	}

	public static String getSetMD5(List<MoleculeProperty> list)
	{
		return getSetMD5(list, "");
	}

	public static String getSetMD5(List<MoleculeProperty> list, String additionalParam)
	{
		return MoleculePropertySetUtil.getMD5(getSets(list), additionalParam);
	}

	public static List<MoleculePropertySet> getSets(List<MoleculeProperty> list)
	{
		List<MoleculePropertySet> sets = new ArrayList<MoleculePropertySet>();
		for (MoleculeProperty p : list)
			if (sets.indexOf(p.getMoleculePropertySet()) == -1)
				sets.add(p.getMoleculePropertySet());
		return sets;
	}

	public static boolean hasUniqueValues(List<MoleculeProperty> list, DatasetFile data)
	{
		for (MoleculeProperty p : list)
			if (!hasUniqueValue(p, data))
				return false;
		return true;
	}

	public static boolean hasUniqueValue(MoleculeProperty prop, DatasetFile data)
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
