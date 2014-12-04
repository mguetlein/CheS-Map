package org.chesmapper.map.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertyOwner;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.mg.javalib.dist.SimilarityMeasure;
import org.mg.javalib.dist.SimilartiyCache;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.DistanceMatrix;

public class DistanceUtil
{
	public static double distance(CompoundData c1, CompoundData c2, List<CompoundProperty> props)
	{
		double d1[] = new double[props.size()];
		double d2[] = new double[props.size()];
		int count = 0;
		for (CompoundProperty p : props)
		{
			if (p instanceof NumericProperty)
			{
				d1[count] = c1.getNormalizedValueCompleteDataset((NumericProperty) p);
				d2[count++] = c2.getNormalizedValueCompleteDataset((NumericProperty) p);
			}
			else
			{
				d1[count] = 0;
				if (c1.getStringValue((NominalProperty) p).equals(c2.getStringValue((NominalProperty) p)))
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
		return c.hashCode() + "#" + props.hashCode();
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
				if (p instanceof NumericProperty)
					throw new IllegalStateException();
				NominalProperty np = (NominalProperty) p;
				if (c.getStringValue(np) == null)
					b[i++] = null;
				else
				{
					String dom[] = np.getDomain();
					if (dom.length != 2)
						throw new IllegalStateException();
					b[i++] = c.getStringValue(np).equals(dom[1]);
				}
			}
			booleanValues.put(key, b);
		}
		return booleanValues.get(key);
	}

	public static boolean isBoolean(List<CompoundProperty> props)
	{
		for (CompoundProperty p : props)
		{
			if (p instanceof NumericProperty)
				return false;
			if (((NominalProperty) p).getDomain().length != 2)
				return false;
		}
		return true;
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
