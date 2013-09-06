package data;

import java.util.List;

import util.ArrayUtil;
import util.DistanceMatrix;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;
import dataInterface.CompoundPropertyOwner;

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
