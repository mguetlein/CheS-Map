package rscript;

import io.RUtil;

import java.util.ArrayList;
import java.util.List;

import util.ArrayUtil;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;

public class ExportRUtil
{
	public static void toRTable(Iterable<MoleculeProperty> features, List<String[]> featureValues,
			String destinationFile)
	{
		// transpose
		List<String[]> featureValues1 = new ArrayList<String[]>();
		for (@SuppressWarnings("unused")
		MoleculeProperty feature : features)
			featureValues1.add(new String[featureValues.size()]);
		for (int i = 0; i < featureValues.size(); i++)
			for (int j = 0; j < featureValues1.size(); j++)
				featureValues1.get(j)[i] = featureValues.get(i)[j];

		//replace nominal features
		List<String> featureNames2 = new ArrayList<String>();
		List<String[]> featureValues2 = new ArrayList<String[]>();
		int count = 0;
		for (MoleculeProperty feature : features)
		{
			if (feature.getType() != Type.NOMINAL
					|| (feature.getNominalDomain().length == 2 && feature.getNominalDomain()[0].equals("0") && feature
							.getNominalDomain()[1].equals("1")))
			{
				featureNames2.add(feature.getName());
				featureValues2.add(featureValues1.get(count));
			}
			else
			{
				System.err.println("Transforming nominal feature: " + feature.getName() + " "
						+ ArrayUtil.toString(feature.getNominalDomain()));

				for (Object val : feature.getNominalDomain())
				{
					String name = feature.getName() + "_is_" + val;
					featureNames2.add(name);
					String vals[] = new String[featureValues1.get(count).length];
					for (int i = 0; i < vals.length; i++)
					{
						if (val.toString().equals(featureValues1.get(count)[i]))
							vals[i] = "1";
						else
							vals[i] = "0";
					}
					featureValues2.add(vals);
					System.err.println("-> new feat: " + name + " " + ArrayUtil.toString(vals));
					if (feature.getNominalDomain().length == 2)
						break;
				}
			}
			count++;
		}
		RUtil.toRTable(featureNames2, featureValues2, destinationFile);
	}
}
