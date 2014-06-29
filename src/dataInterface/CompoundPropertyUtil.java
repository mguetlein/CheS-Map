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
import data.DatasetFile;
import data.cdk.CDKProperty;
import data.obdesc.OBDescriptorPropertySet;
import data.obfingerprints.OBFingerprintSet;
import dataInterface.CompoundProperty.Type;
import freechart.FreeChartUtil;

public class CompoundPropertyUtil
{
	public static boolean isExportedFPProperty(CompoundProperty p)
	{
		return (p.getType() != Type.NUMERIC && p.getNominalDomain().length == 2 && p.getNominalDomain()[0].equals("0")
				&& p.getNominalDomain()[1].equals("1") && p.toString().matches("^OB-.*:.*"));
	}

	//	public static boolean isExportedFPPropertyInMappedDataset(CompoundProperty p)
	//	{
	//		return isExportedFPProperty(p, Settings.MAPPED_DATASET);
	//	}

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
		if (p instanceof OBDescriptorPropertySet)
			return "OB:" + p.toString();
		if (p.getCompoundPropertySet() instanceof OBFingerprintSet)
			return "OB-" + ((OBFingerprintSet) p.getCompoundPropertySet()).getOBType() + ":" + p.toString();
		return p.toString();
	}

	public static int computeNumDistinct(Double values[])
	{
		return DoubleArraySummary.create(values).getNumDistinct();
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
						d[count++] = p.getNormalizedMedianInCompleteDataset() + "";
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

	public static final Color[] AVAILABLE_COLORS = FreeChartUtil.BRIGHT_COLORS;
	public static Color[] CLUSTER_COLORS;
	public static final Color[] DEFAULT_HIGHILIGHT_MATCH_COLORS;
	public static Color[] HIGHILIGHT_MATCH_COLORS;

	static
	{
		//start with blue instead of red
		AVAILABLE_COLORS[0] = getLowValueColor();
		AVAILABLE_COLORS[1] = FreeChartUtil.BRIGHT_RED;
		CLUSTER_COLORS = AVAILABLE_COLORS;
		DEFAULT_HIGHILIGHT_MATCH_COLORS = new Color[] { getLowValueColor(), getHighValueColor(), Color.ORANGE };
		HIGHILIGHT_MATCH_COLORS = DEFAULT_HIGHILIGHT_MATCH_COLORS;
	}

	public static Color getClusterColor(int index)
	{
		return CLUSTER_COLORS[index % CLUSTER_COLORS.length];
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

	private static HashMap<String, Color[]> mapping = new HashMap<String, Color[]>();

	public static Color[] getNominalColors(CompoundProperty p)
	{
		if (p.getType() != Type.NOMINAL)
			throw new IllegalArgumentException();

		String key = p.toString() + "#" + p.getNominalDomain().hashCode() + "#"
				+ (p.isSmartsProperty() ? CompoundPropertyUtil.HIGHILIGHT_MATCH_COLORS : p.getHighlightColorSequence());
		if (!mapping.containsKey(key))
		{
			Color col[];
			if (p.isSmartsProperty())
				col = CompoundPropertyUtil.HIGHILIGHT_MATCH_COLORS;
			else
			{
				col = p.getHighlightColorSequence();
				if (col == null)
					col = AVAILABLE_COLORS;
				while (p.getNominalDomain().length > col.length)
					col = ArrayUtil.concat(col, col);
				if (p.getNominalDomain().length < col.length)
					col = Arrays.copyOfRange(col, 0, p.getNominalDomain().length);
			}
			mapping.put(key, col);
		}
		return mapping.get(key);
	}

	public static Color getNominalColor(CompoundProperty p, String val)
	{
		int index = ArrayUtil.indexOf(p.getNominalDomain(), val);
		if (index == -1)
			throw new IllegalStateException(val + " not found in " + ArrayUtil.toString(p.getNominalDomain())
					+ " for property " + p);
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

	//	public static boolean hasUniqueValues(List<CompoundProperty> list, DatasetFile data)
	//	{
	//		for (CompoundProperty p : list)
	//			if (!hasUniqueValue(p, data))
	//				return false;
	//		return true;
	//	}

	//	public static boolean hasUniqueValue(CompoundProperty prop, DatasetFile data)
	//	{
	//		boolean first = true;
	//		Object value = null;
	//		for (Object val : (prop.getType() == Type.NUMERIC ? prop.getDoubleValues(data) : prop.getStringValues(data)))
	//		{
	//			if (first)
	//			{
	//				value = val;
	//				first = false;
	//			}
	//			else
	//			{
	//				if (!ObjectUtil.equals(value, val))
	//					return false;
	//			}
	//		}
	//		return true;
	//	}

	public static void determineRedundantFeatures(List<CompoundProperty> features)
	{
		HashMap<CompoundProperty, CompoundProperty> redundant = getRedundantFeatures(features, null);
		for (CompoundProperty p : redundant.keySet())
			p.setRedundantProp(redundant.get(p));
	}

	public static HashMap<CompoundProperty, CompoundProperty> getRedundantFeatures(List<CompoundProperty> features,
			int compoundSubset[])
	{
		List<CompoundProperty> notRedundant = new ArrayList<CompoundProperty>();
		HashMap<CompoundProperty, Integer> redundantIndices = new HashMap<CompoundProperty, Integer>();
		for (int i = 0; i < features.size(); i++)
		{
			CompoundProperty p = features.get(i);
			int matchIndex = -1;
			for (int j = 0; j < notRedundant.size(); j++)
			{
				CompoundProperty p2 = notRedundant.get(j);
				if (isRedundant(p, p2, compoundSubset))
				{
					matchIndex = j;
					break;
				}
			}
			if (matchIndex == -1)
				notRedundant.add(p);
			else
			{
				CompoundProperty pMatch = notRedundant.get(matchIndex);
				if ((p.isSmartsProperty() && !pMatch.isSmartsProperty()) // only the new p is smarts
						|| (p.isSmartsProperty() && pMatch.isSmartsProperty() // if both are smarts 
						// use the shorter one
						&& SmartsUtil.getLength(p.getSmarts()) < SmartsUtil.getLength(pMatch.getSmarts())))
				{
					notRedundant.set(matchIndex, p); // replace pMatch
					redundantIndices.put(pMatch, matchIndex);
				}
				else
					redundantIndices.put(p, matchIndex);
			}
		}
		HashMap<CompoundProperty, CompoundProperty> redundant = new HashMap<CompoundProperty, CompoundProperty>();
		for (CompoundProperty p : redundantIndices.keySet())
		{
			redundant.put(p, notRedundant.get(redundantIndices.get(p)));
			if (p.isSmartsProperty() && redundant.get(p).isSmartsProperty())
				Settings.LOGGER.debug("skip " + p.getSmarts() + " because of " + redundant.get(p).getSmarts());
		}
		return redundant;
	}

	private static boolean isRedundant(CompoundProperty p1, CompoundProperty p2, int compoundSubset[])
	{
		if (p1.getType() == Type.NUMERIC && p2.getType() == Type.NUMERIC)
			return ArrayUtil.equals(p1.getDoubleValuesInCompleteDataset(), p2.getDoubleValuesInCompleteDataset(),
					compoundSubset);
		else if (p1.getType() == Type.NOMINAL && p2.getType() == Type.NOMINAL)
			return ArrayUtil.redundant(p1.getStringValuesInCompleteDataset(), p2.getStringValuesInCompleteDataset(),
					compoundSubset);
		else
			return false;
	}
}
