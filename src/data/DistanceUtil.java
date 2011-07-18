package data;

import java.util.ArrayList;
import java.util.List;

import util.ArrayUtil;
import util.DistanceMatrix;
import dataInterface.MoleculeProperty;
import dataInterface.MolecularPropertyOwner;

public class DistanceUtil
{
	public static List<double[]> values(List<MoleculeProperty> props, List<MolecularPropertyOwner> values, boolean normalized)
	{
		List<double[]> v = new ArrayList<double[]>();
		for (MolecularPropertyOwner vv : values)
		{
			double d[] = new double[props.size()];
			int count = 0;
			for (MoleculeProperty p : props)
				d[count++] = vv.getValue(p, normalized);
			v.add(d);
		}
		return v;
	}

	public static double distance(MolecularPropertyOwner c1, MolecularPropertyOwner c2, List<MoleculeProperty> props, boolean normalized)
	{
		double d1[] = new double[props.size()];
		double d2[] = new double[props.size()];
		int count = 0;
		for (MoleculeProperty p : props)
		{
			d1[count] = c1.getValue(p, normalized);
			d2[count++] = c2.getValue(p, normalized);
		}
		return ArrayUtil.euclDistance(d1, d2);
	}

	public static DistanceMatrix<MolecularPropertyOwner> computeDistances(List<MolecularPropertyOwner> instances, List<MoleculeProperty> props)
	{
		DistanceMatrix<MolecularPropertyOwner> m = new DistanceMatrix<MolecularPropertyOwner>();
		for (int i = 0; i < instances.size() - 1; i++)
			for (int j = i + 1; j < instances.size(); j++)
				m.setDistance(instances.get(i), instances.get(j),
						DistanceUtil.distance(instances.get(i), instances.get(j), props, true));
		return m;
	}
}
