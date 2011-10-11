package dataInterface;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;

import util.ArrayUtil;
import util.ColorUtil;
import dataInterface.MoleculeProperty.Type;
import freechart.FreeChartUtil;

public class MoleculePropertyUtil
{
	private static HashMap<MoleculeProperty, Color[]> mapping = new HashMap<MoleculeProperty, Color[]>();

	public static Color[] AVAILABLE_COLORS = ColorUtil.darker(FreeChartUtil.COLORS);
	static
	{
		Color col = AVAILABLE_COLORS[0];
		AVAILABLE_COLORS[0] = AVAILABLE_COLORS[1];
		AVAILABLE_COLORS[1] = col;
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
		Object s[] = ArrayUtil.toStringArray(p.getNominalDomain());
		int index = ArrayUtil.indexOf(s, val);
		if (index == -1)
			throw new IllegalStateException(val + " not found in " + ArrayUtil.toString(s));
		return getNominalColors(p)[index];
	}
}
