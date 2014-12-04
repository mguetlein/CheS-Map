package org.chesmapper.map.rscript;

import java.util.ArrayList;
import java.util.List;

import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.main.Settings;
import org.mg.javalib.io.RUtil;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.ObjectUtil;

public class ExportRUtil
{
	public static class NumericTable
	{
		public List<String> featureNames;
		public List<String[]> featureValues;

		private List<String[]> compoundValues;

		public List<String[]> getCompoundValues()
		{
			if (compoundValues == null)
			{
				compoundValues = new ArrayList<String[]>();
				int numCompounds = featureValues.get(0).length;
				int numFeatures = featureValues.size();
				for (int i = 0; i < numCompounds; i++)
				{
					String[] s = new String[numFeatures];
					for (int j = 0; j < numFeatures; j++)
						s[j] = featureValues.get(j)[i];
					compoundValues.add(s);
				}
			}
			return compoundValues;
		}
	}

	public static NumericTable toNumericTable(Iterable<CompoundProperty> features, List<String[]> featureValues)
	{
		// transpose
		List<String[]> featureValues1 = new ArrayList<String[]>();
		for (@SuppressWarnings("unused")
		CompoundProperty feature : features)
			featureValues1.add(new String[featureValues.size()]);
		for (int i = 0; i < featureValues.size(); i++)
			for (int j = 0; j < featureValues1.size(); j++)
				featureValues1.get(j)[i] = featureValues.get(i)[j];

		//replace nominal features
		List<String> featureNames2 = new ArrayList<String>();
		List<String[]> featureValues2 = new ArrayList<String[]>();
		int count = 0;
		for (CompoundProperty feature : features)
		{
			if (feature instanceof NumericProperty
					|| (((NominalProperty) feature).getDomain().length == 2
							&& ((NominalProperty) feature).getDomain()[0].equals("0") && ((NominalProperty) feature)
								.getDomain()[1].equals("1")))
			{
				featureNames2.add(feature.getName());
				featureValues2.add(featureValues1.get(count));
			}
			else
			{
				Settings.LOGGER.info("Transforming nominal feature: " + feature.getName() + " "
						+ ArrayUtil.toString(((NominalProperty) feature).getDomain()));

				for (String val : ((NominalProperty) feature).getDomain())
				{
					String name = feature.getName() + "_is_" + val;
					featureNames2.add(name);
					String vals[] = new String[featureValues1.get(count).length];
					for (int i = 0; i < vals.length; i++)
					{
						if (ObjectUtil.equals(val, featureValues1.get(count)[i]))
							vals[i] = "1";
						else
							vals[i] = "0";
					}
					featureValues2.add(vals);
					Settings.LOGGER.info("-> new feat: " + name + " " + ArrayUtil.toString(vals));
					if (((NominalProperty) feature).getDomain().length == 2)
						break;
				}
			}
			count++;
		}
		NumericTable t = new NumericTable();
		t.featureNames = featureNames2;
		t.featureValues = featureValues2;
		return t;
	}

	public static void toRTable(Iterable<CompoundProperty> features, List<String[]> featureValues,
			String destinationFile)
	{
		NumericTable t = toNumericTable(features, featureValues);
		Settings.LOGGER.info("store features in " + destinationFile);
		RUtil.toRTable(t.featureNames, t.featureValues, destinationFile);

	}
}
