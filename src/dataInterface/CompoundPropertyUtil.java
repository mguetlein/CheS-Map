package dataInterface;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.Settings;
import util.ArrayUtil;
import util.DoubleArraySummary;
import util.ObjectUtil;
import data.DatasetFile;
import data.cdk.CDKProperty;
import data.obdesc.OBDescriptorProperty;
import data.obfingerprints.OBFingerprintProperty;
import dataInterface.CompoundProperty.Type;
import freechart.FreeChartUtil;

public class CompoundPropertyUtil
{
	public static boolean isExportedFPProperty(CompoundProperty p, DatasetFile d)
	{
		return (p.getType() != Type.NUMERIC && p.getNominalDomain(d).length == 2
				&& p.getNominalDomain(d)[0].equals("0") && p.getNominalDomain(d)[1].equals("1") && p.toString()
				.matches("^OB-.*:.*"));
	}

	public static boolean isExportedFPPropertyInMappedDataset(CompoundProperty p)
	{
		return isExportedFPProperty(p, Settings.MAPPED_DATASET);
	}

	public static String stripExportString(CompoundProperty p)
	{
		if (p.toString().startsWith("CDK:"))
			return p.toString().substring(4);
		if (p.toString().startsWith("OB:"))
			return p.toString().substring(3);
		Matcher m = Pattern.compile("^OB-.*:(.*)").matcher(p.toString());
		if (m.matches())
			return m.group(1);
		return p.toString();
	}

	public static String propToExportString(CompoundProperty p)
	{
		if (p instanceof CDKProperty)
			return "CDK:" + p.toString();
		if (p instanceof OBDescriptorProperty)
			return "OB:" + p.toString();
		if (p instanceof OBFingerprintProperty)
			return "OB-" + ((OBFingerprintProperty) p).getOBType() + ":" + p.toString();
		return p.toString();
	}

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

	public static List<String[]> valuesReplaceNullWithMedian(List<CompoundProperty> props, List<CompoundData> values,
			DatasetFile dataset)
	{
		List<String[]> v = new ArrayList<String[]>();
		for (CompoundData vv : values)
		{
			String d[] = new String[props.size()];
			int count = 0;
			for (CompoundProperty p : props)
			{
				if (p.getType() == Type.NUMERIC)
				{
					Double val = vv.getNormalizedValueCompleteDataset(p);
					if (val == null)
						d[count++] = p.getNormalizedMedian(dataset) + "";
					else
						d[count++] = val + "";
				}
				else
					d[count++] = vv.getStringValue(p);
			}
			v.add(d);
		}
		return v;
	}

	private static Color[] AVAILABLE_COLORS = FreeChartUtil.BRIGHT_COLORS;

	static
	{
		//start with blue instead of red
		AVAILABLE_COLORS[0] = getLowValueColor();
		AVAILABLE_COLORS[1] = FreeChartUtil.BRIGHT_RED;
	}

	public static Color getClusterColor(int index)
	{
		return AVAILABLE_COLORS[index % AVAILABLE_COLORS.length];
	}

	public static Color getHighValueColor()
	{
		return FreeChartUtil.BRIGHT_RED; //Color.RED;
	}

	public static Color getLowValueColor()
	{
		return new Color(100, 100, 255);
		//return FreeChartUtil.BRIGHT_BLUE;
	}

	public static Color getNumericChartColor()
	{
		return getLowValueColor();
	}

	public static Color getNumericChartHighlightColor()
	{
		return FreeChartUtil.BRIGHT_RED;
	}

	public static Color getNullValueColor()
	{
		return Color.DARK_GRAY;
	}

	public static Color getSmartsColorMatch()
	{
		return getHighValueColor();
	}

	public static Color getSmartsColorNoMatch()
	{
		return getLowValueColor();
	}

	private static HashMap<String, Color[]> mapping = new HashMap<String, Color[]>();

	public static Color[] getNominalColors(CompoundProperty p)
	{
		if (p.getType() != Type.NOMINAL)
			throw new IllegalArgumentException();

		String key = p.toString() + "#" + p.getNominalDomainInMappedDataset().hashCode();
		if (!mapping.containsKey(key))
		{
			Color col[];
			if (p.isSmartsProperty())
				col = new Color[] { getLowValueColor(), getHighValueColor() };
			else if (p.getNominalDomainInMappedDataset().length == 2
					&& p.getNominalDomainInMappedDataset()[0].equals("active"))
				col = new Color[] { getHighValueColor(), getLowValueColor() };
			else
			{
				col = AVAILABLE_COLORS;
				while (p.getNominalDomainInMappedDataset().length > col.length)
					col = ArrayUtil.concat(col, AVAILABLE_COLORS);
				if (p.getNominalDomainInMappedDataset().length < col.length)
					col = Arrays.copyOfRange(col, 0, p.getNominalDomainInMappedDataset().length);
			}
			mapping.put(key, col);
		}
		return mapping.get(key);
	}

	public static Color getNominalColor(CompoundProperty p, String val)
	{
		int index = ArrayUtil.indexOf(p.getNominalDomainInMappedDataset(), val);
		if (index == -1)
			throw new IllegalStateException(val + " not found in "
					+ ArrayUtil.toString(p.getNominalDomainInMappedDataset()) + " for property " + p);
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
