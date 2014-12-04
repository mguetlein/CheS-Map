package org.chesmapper.map.dataInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.mg.javalib.util.Binning;
import org.mg.javalib.util.CountedSet;

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
		if (selected == null || selected.length == 0)
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

		Binning binning;

		private NumericSingleHelper(double a[])
		{
			binning = new Binning(a, 20, false);
		}

		public static NumericSingleHelper get(double a[])
		{
			if (!map.containsKey(a))
				map.put(a, new NumericSingleHelper(a));
			return map.get(a);
		}

		/**
		 * specificity for numeric single prop is performed by binning + chi-square
		 * as prop-dense implementation in math3 was not willing to work 
		 */
		public double specificity(double s)
		{
			long all[] = binning.getAllCounts();
			long selected[] = binning.getSelectedCounts(s);
			return nominalSpecificty(selected, all);
		}
	}

	public static double numericSingleSpecificty(double selected, double[] all)
	{
		return NumericSingleHelper.get(all).specificity(selected);
	}

	public static void main(String[] args) throws IOException
	{
		double[] d = new NormalDistribution(0, 5).sample(500);
		for (Double i : new double[] { -10, -5.0, 0.0, 5.0, 10 })
			System.out.println(i + " " + NumericSingleHelper.get(d).specificity(i));
	}
}
