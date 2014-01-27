package data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.Settings;
import util.ArrayUtil;
import util.DistanceMatrix;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;
import dataInterface.CompoundPropertyOwner;
import dist.SimilarityMeasure;
import dist.SimilartiyCache;

public class DistanceUtil
{
	public static double distance(CompoundData c1, CompoundData c2, List<CompoundProperty> props)
	{
		double d1[] = new double[props.size()];
		double d2[] = new double[props.size()];
		int count = 0;
		for (CompoundProperty p : props)
		{
			if (p.getType() == Type.NUMERIC)
			{
				d1[count] = c1.getNormalizedValueCompleteDataset(p);
				d2[count++] = c2.getNormalizedValueCompleteDataset(p);
			}
			else
			{
				d1[count] = 0;
				if (c1.getStringValue(p).equals(c2.getStringValue(p)))
					d2[count++] = 0;
				else
					d2[count++] = 1;
			}
		}
		return ArrayUtil.euclDistance(d1, d2);
	}

	private static HashMap<String, Boolean[]> booleanValues = new HashMap<String, Boolean[]>();

	private static String valsKey(CompoundData c, List<CompoundProperty> props)
	{
		return Settings.MAPPED_DATASET.hashCode() + "#" + c.hashCode() + "#" + props.hashCode();
	}

	private static Boolean[] booleanValues(CompoundData c, List<CompoundProperty> props)
	{
		String key = valsKey(c, props);
		if (!booleanValues.containsKey(key))
		{
			Boolean b[] = new Boolean[props.size()];
			int i = 0;
			for (CompoundProperty p : props)
			{
				if (p.getType() == Type.NUMERIC)
					throw new IllegalStateException();
				if (c.getStringValue(p) == null)
					b[i++] = null;
				else
				{
					String dom[] = p.getNominalDomainInMappedDataset();
					if (dom.length != 2)
						throw new IllegalStateException();
					b[i++] = c.getStringValue(p).equals(dom[1]);
				}
			}
			booleanValues.put(key, b);
		}
		return booleanValues.get(key);
	}

	public static Double similarity(List<CompoundData> instances, List<CompoundProperty> props,
			SimilarityMeasure<Boolean> sim)
	{
		List<Boolean[]> vals = new ArrayList<Boolean[]>();
		for (CompoundData c : instances)
			vals.add(booleanValues(c, props));
		return SimilartiyCache.get(sim).similarity(vals);
	}

	public static DistanceMatrix<CompoundPropertyOwner> computeDistances(List<CompoundData> instances,
			List<CompoundProperty> props)
	{
		DistanceMatrix<CompoundPropertyOwner> m = new DistanceMatrix<CompoundPropertyOwner>();
		for (int i = 0; i < instances.size() - 1; i++)
			for (int j = i + 1; j < instances.size(); j++)
				m.setDistance(instances.get(i), instances.get(j),
						DistanceUtil.distance(instances.get(i), instances.get(j), props));
		return m;
	}
}
