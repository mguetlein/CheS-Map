package dataInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.stat.inference.TestUtils;

import util.CountedSet;
import util.DoubleArraySummary;

public class CompoundPropertySpecificity
{
	public static final double NO_SPEC_AVAILABLE = 1;

	public static <T> long[] nominalCounts(List<T> values, CountedSet<T> all)
	{
		long[] counts = new long[values.size()];
		int i = 0;
		for (T v : values)
			counts[i++] = all.getCount(v);
		return counts;
	}

	public static <T> long[] nominalCount(List<T> values, T value)
	{
		if (value == null)
			return null;
		long[] counts = new long[values.size()];
		int i = 0;
		for (T v : values)
			counts[i++] = value.equals(v) ? 1 : 0;
		return counts;
	}

	public static <T> double nominalSpecificty(long[] selected, long[] all)
	{
		if (selected == null || all.length == 1)
			return NO_SPEC_AVAILABLE;
		if (selected.length != all.length || all.length == 0)
			throw new IllegalArgumentException();
		double d = TestUtils.chiSquareTestDataSetsComparison(selected, all);
		if (Double.isNaN(d))
			return NO_SPEC_AVAILABLE;
		else
			return d;
	}

	public static double numericMultiSpecificty(double[] selected, double[] all)
	{
		if (selected.length == 0)
			return NO_SPEC_AVAILABLE;
		if (selected.length == 1)
			return numericSingleSpecificty(selected[0], all);

		List<double[]> l = new ArrayList<double[]>();
		l.add(selected);
		l.add(all);

		double d = TestUtils.oneWayAnovaPValue(l);
		if (Double.isNaN(d))
			return NO_SPEC_AVAILABLE;
		else
			return d;
	}

	private static class NumericSingleHelper
	{
		private static HashMap<double[], NumericSingleHelper> map = new HashMap<double[], CompoundPropertySpecificity.NumericSingleHelper>();

		double med;
		double maxDistToMedian;
		double minDistToMedian;

		private NumericSingleHelper(double a[])
		{
			med = DoubleArraySummary.create(a).getMedian();
			double distToMed[] = new double[a.length];
			for (int i = 0; i < distToMed.length; i++)
				distToMed[i] = Math.abs(a[i] - med);
			DoubleArraySummary distToMedSummary = DoubleArraySummary.create(distToMed);
			maxDistToMedian = distToMedSummary.getMax();
			minDistToMedian = distToMedSummary.getMin();
		}

		public static NumericSingleHelper get(double a[])
		{
			if (!map.containsKey(a))
				map.put(a, new NumericSingleHelper(a));
			return map.get(a);
		}

		/**
		 * idea: the specificty is 1 - the relative distance to the median
		 * i.e. the farer the value is away from the median, the higher the spec / the smaller the value
		 */
		public double specificity(double s)
		{
			if (maxDistToMedian == minDistToMedian)
				return NO_SPEC_AVAILABLE;
			double dist = Math.abs(s - med);
			if (dist >= maxDistToMedian)
				return 0;
			if (dist <= minDistToMedian)
				return 1;
			dist -= minDistToMedian;
			double d = 1 - dist / (maxDistToMedian - minDistToMedian);
			if (Double.isNaN(d))
				return NO_SPEC_AVAILABLE;
			else
				return d;
		}
	}

	public static double numericSingleSpecificty(double selected, double[] all)
	{
		return NumericSingleHelper.get(all).specificity(selected);
	}

	public static void main(String[] args)
	{

		//		System.out.println(numericMultiSpecificty(new NormalDistribution(0, 1).sample(1000), new NormalDistribution(
		//				0.2, 1).sample(1000)));
		System.out.println(numericSingleSpecificty(0, new double[] { 0, 0, 0, 0, 0, 6, 6, 6, 10, 12 }));
	}
}
