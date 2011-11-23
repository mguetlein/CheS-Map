package data;

import java.util.List;

import util.ArrayUtil;
import util.DistanceMatrix;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;

public class DistanceUtil
{
	public static double distance(MolecularPropertyOwner c1, MolecularPropertyOwner c2, List<MoleculeProperty> props)
	{
		double d1[] = new double[props.size()];
		double d2[] = new double[props.size()];
		int count = 0;
		for (MoleculeProperty p : props)
		{
			if (p.getType() == Type.NUMERIC)
			{
				d1[count] = c1.getNormalizedValue(p);
				d2[count++] = c2.getNormalizedValue(p);
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

	public static DistanceMatrix<MolecularPropertyOwner> computeDistances(List<MolecularPropertyOwner> instances,
			List<MoleculeProperty> props)
	{
		DistanceMatrix<MolecularPropertyOwner> m = new DistanceMatrix<MolecularPropertyOwner>();
		for (int i = 0; i < instances.size() - 1; i++)
			for (int j = i + 1; j < instances.size(); j++)
				m.setDistance(instances.get(i), instances.get(j),
						DistanceUtil.distance(instances.get(i), instances.get(j), props));
		return m;
	}
}
