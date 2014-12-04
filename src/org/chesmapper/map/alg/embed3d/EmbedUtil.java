package org.chesmapper.map.alg.embed3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.vecmath.Vector3f;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertyOwner;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.main.TaskProvider;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.ObjectUtil;
import org.mg.javalib.util.StopWatchUtil;
import org.mg.javalib.util.Vector3fUtil;

public class EmbedUtil
{
	public static double computeCorrelation(CorrelationType type, List<Vector3f> positions,
			DistanceMatrix featureDistanceMatrix)
	{
		double d1[][] = featureDistanceMatrix.getNormalizedValues();
		double d2[][] = euclMatrix(positions);
		normalizeDistanceMatrix(d2);
		switch (type)
		{
			case RSquare:
				return computeRSquare(d1, d2);
			case CCC:
				return computeCCC(d1, d2);
			case Pearson:
				return computePearson(d1, d2);
			default:
				throw new Error("not yet implemented: " + type);
		}

	}

	private static double computePearson(double[][] d1, double[][] d2)
	{
		double[] a1 = ArrayUtil.concatUpperTriangular(d1);
		double[] a2 = ArrayUtil.concatUpperTriangular(d2);
		//		StringBuffer s = new StringBuffer();
		//		for (int i = 0; i < a2.length; i++)
		//			s.append(a1[i] + "," + a2[i] + "\n");
		//		FileUtil.writeStringToFile("/tmp/pearson.csv", s.toString());
		PearsonsCorrelation pc = new PearsonsCorrelation();
		return pc.correlation(a1, a2);
	}

	private static double[] computePearsons(double[][] d1, double[][] d2)
	{
		double a1[][] = ArrayUtil.removeDiagonale(d1);
		double a2[][] = ArrayUtil.removeDiagonale(d2);
		double[] p = new double[d1.length];
		PearsonsCorrelation pc = new PearsonsCorrelation();
		for (int i = 0; i < p.length; i++)
			p[i] = pc.correlation(a1[i], a2[i]);
		return p;
	}

	public static double[] computeCorrelations(CorrelationType type, List<Vector3f> positions,
			DistanceMatrix featureDistanceMatrix)
	{
		double d1[][] = featureDistanceMatrix.getNormalizedValues();
		double d2[][] = euclMatrix(positions);
		normalizeDistanceMatrix(d2);
		switch (type)
		{
			case CCC:
				return computeCCCs(d1, d2);
			case Pearson:
				return computePearsons(d1, d2);
			default:
				throw new Error("not yet implemented: " + type);
		}
	}

	private static double computeRSquare(double[][] m1, double[][] m2)
	{
		if (m1.length != m1[0].length || m2.length != m2[0].length || m1.length != m2.length)
			throw new IllegalArgumentException();

		double ss_err = 0;
		double ss_tot = 0;
		double m1_mean = 0;
		int count = 0;
		for (int i = 0; i < m1.length; i++)
			for (int j = 0; j < m1.length; j++)
				if (i != j)
				{
					m1_mean += m1[i][j];
					count++;
				}
		m1_mean /= (double) count;
		for (int i = 0; i < m1.length; i++)
			for (int j = 0; j < m1.length; j++)
				if (i != j)
				{
					ss_err += Math.pow(m1[i][j] - m2[i][j], 2);
					ss_tot += Math.pow(m1[i][j] - m1_mean, 2);
				}
		return 1 - ss_err / ss_tot;
	}

	private static double[] computeCCCs(double[][] m1, double[][] m2)
	{
		if (m1.length != m1[0].length || m2.length != m2[0].length || m1.length != m2.length)
			throw new IllegalArgumentException();

		double ccc[] = new double[m1.length];

		for (int k = 0; k < ccc.length; k++)
		{
			double m1_mean = 0;
			double m2_mean = 0;
			int count = 0;
			for (int i = 0; i < m1.length; i++)
				if (i != k)
				{
					m1_mean += m1[i][k];
					m2_mean += m2[i][k];
					m1_mean += m1[k][i];
					m2_mean += m2[k][i];
					count += 2;
				}
			m1_mean /= (double) count;
			m2_mean /= (double) count;

			double numerator = 0;
			for (int i = 0; i < m1.length; i++)
				if (i != k)
				{
					numerator += (m1[i][k] - m1_mean) * (m2[i][k] - m2_mean);
					numerator += (m1[k][i] - m1_mean) * (m2[k][i] - m2_mean);
				}
			numerator *= 2;

			double m1_ss = 0;
			double m2_ss = 0;
			for (int i = 0; i < m1.length; i++)
				if (i != k)
				{
					m1_ss += Math.pow((m1[i][k] - m1_mean), 2);
					m2_ss += Math.pow((m2[i][k] - m2_mean), 2);
					m1_ss += Math.pow((m1[k][i] - m1_mean), 2);
					m2_ss += Math.pow((m2[k][i] - m2_mean), 2);
				}
			double denominator = m1_ss + m2_ss;
			denominator += count * Math.pow((m1_mean - m2_mean), 2);
			ccc[k] = numerator / denominator;
			if (Double.isInfinite(ccc[k]) || Double.isNaN(ccc[k]))
				ccc[k] = 0;

		}
		return ccc;
	}

	private static double computeCCC(double[][] m1, double[][] m2)
	{
		double m1_mean = 0;
		double m2_mean = 0;
		int count = 0;
		for (int i = 0; i < m1.length; i++)
			for (int j = 0; j < m1.length; j++)
				if (i != j)
				{
					m1_mean += m1[i][j];
					m2_mean += m2[i][j];
					count++;
				}
		m1_mean /= (double) count;
		m2_mean /= (double) count;

		double numerator = 0;
		for (int i = 0; i < m1.length; i++)
			for (int j = 0; j < m1.length; j++)
				if (i != j)
					numerator += (m1[i][j] - m1_mean) * (m2[i][j] - m2_mean);
		numerator *= 2;

		double m1_ss = 0;
		double m2_ss = 0;
		for (int i = 0; i < m1.length; i++)
			for (int j = 0; j < m1.length; j++)
				if (i != j)
				{
					m1_ss += Math.pow((m1[i][j] - m1_mean), 2);
					m2_ss += Math.pow((m2[i][j] - m2_mean), 2);
				}

		double denominator = m1_ss + m2_ss;
		denominator += count * Math.pow((m1_mean - m2_mean), 2);
		double ccc = numerator / denominator;
		if (Double.isInfinite(ccc) || Double.isNaN(ccc))
			return 0;
		return ccc;
	}

	private static void normalizeDistanceMatrix(double d[][])
	{
		//hack to make normalization work: fill diagonal values with something from value range 
		for (int i = 0; i < d.length; i++)
			d[i][i] = d[0][1];
		ArrayUtil.normalize(d);
		for (int i = 0; i < d.length; i++)
			d[i][i] = 0.0;
		//		Settings.LOGGER.println(ArrayUtil.toString(d));
	}

	public static double[][] euclMatrix(List<CompoundData> instances, List<CompoundProperty> features)
	{
		TaskProvider.debug("Compute euclidean distance matrix");

		HashMap<CompoundPropertyOwner, double[]> vals = new HashMap<CompoundPropertyOwner, double[]>();

		for (int i = 0; i < instances.size(); i++)
		{
			CompoundData c = instances.get(i);

			List<Double> v_i = new ArrayList<Double>();
			for (int k = 0; k < features.size(); k++)
			{
				if (features.get(k) instanceof NumericProperty)
				{
					NumericProperty feature = (NumericProperty) features.get(k);
					Double v = c.getNormalizedValueCompleteDataset(feature);
					if (v == null)
						v = feature.getNormalizedMedian();
					v_i.add(v);
				}
				else
				{
					NominalProperty feature = (NominalProperty) features.get(k);
					if (feature.getDomain().length == 2 && feature.getDomain()[0].equals("0")
							&& feature.getDomain()[1].equals("1"))
					{
						if (c.getStringValue(feature) == null)
							v_i.add(Double.parseDouble(feature.getModeNonNull()));
						else
							v_i.add(Double.parseDouble(c.getStringValue(feature)));
					}
					else
					{
						for (String val : feature.getDomain())
						{
							if (ObjectUtil.equals(c.getStringValue(feature), val))
								v_i.add(1.0);
							else
								v_i.add(0.0);
						}
					}
				}
			}
			//			Settings.LOGGER.println(vals.size() + " " + ListUtil.toString(v_i));
			vals.put(c, ArrayUtil.toPrimitiveDoubleArray(v_i));
		}
		//		for (MolecularPropertyOwner m : vals.keySet())
		//			Settings.LOGGER.println("values: " + m.toString() + " " + ArrayUtil.toString(vals.get(m)));

		double[][] d = new double[instances.size()][instances.size()];
		for (int i = 0; i < instances.size() - 1; i++)
			for (int j = i + 1; j < instances.size(); j++)
			{
				d[i][j] = ArrayUtil.euclDistance(vals.get(instances.get(i)), vals.get(instances.get(j)));
				d[j][i] = d[i][j];
			}
		//		Settings.LOGGER.println(ArrayUtil.toString(d));
		return d;
	}

	private static double[][] euclMatrix(List<Vector3f> positions)
	{
		double[][] d = new double[positions.size()][positions.size()];
		for (int i = 0; i < positions.size() - 1; i++)
			for (int j = i + 1; j < positions.size(); j++)
			{
				d[i][j] = Vector3fUtil.dist(positions.get(i), positions.get(j));
				d[j][i] = d[i][j];
			}
		//		Settings.LOGGER.println(ArrayUtil.toString(d));
		return d;
	}

	public static void main(String args[])
	{
		Random r = new Random();

		int numCompounds = 10000;
		int numValues = 2000;

		List<double[]> l = new ArrayList<double[]>();
		for (int i = 0; i < numCompounds; i++)
		{
			double d[] = new double[numValues];
			for (int k = 0; k < numValues; k++)
				d[k] = r.nextDouble();
			l.add(d);
		}
		StopWatchUtil.start("calc");
		long c = 0;
		for (int i = 0; i < numCompounds - 1; i++)
		{
			for (int j = i + 1; j < numCompounds; j++)
			{
				ArrayUtil.euclDistance(l.get(i), l.get(j));
				c++;
			}
		}
		System.out.println(c);
		StopWatchUtil.stop("calc");
		StopWatchUtil.print();
		//		int ns[] = new int[] { 100 }; //{ 10, 50, 100, 500, 1000 };
		//		int restarts = 30;
		//
		//		double deviation[] = new double[] { 1, 0.5, 0.25, 0.20, 0.15, 0.10, 0.05 };
		//
		//		for (int n : ns)
		//		{
		//			Settings.LOGGER.info(n);
		//
		//			for (double dev : deviation)
		//			{
		//				Settings.LOGGER.info("dev: " + dev);
		//
		//				double[] results = new double[restarts];
		//
		//				for (int k = 0; k < restarts; k++)
		//				{
		//					double d[][] = new double[n][n];
		//					double d2[][] = new double[n][n];
		//
		//					for (int i = 0; i < n; i++)
		//						for (int j = 0; j < n; j++)
		//							if (i != j)
		//							{
		//								d[i][j] = r.nextDouble();
		//								d2[i][j] = d[i][j] + (d[i][j] * dev * (r.nextBoolean() ? 1 : -1));
		//								//d2[i][j] = r.nextDouble();
		//							}
		//
		//					normalizeDistanceMatrix(d);
		//					normalizeDistanceMatrix(d2);
		//					results[k] = computeRSquare(d, d2);
		//				}
		//
		//				Settings.LOGGER.info(DoubleArraySummary.create(results).getMedian());
		//			}
		//			Settings.LOGGER.info();
		//		}
	}

}
