package alg.embed3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.vecmath.Vector3f;

import main.Settings;
import util.ArrayUtil;
import util.DoubleArraySummary;
import util.ObjectUtil;
import util.Vector3fUtil;
import data.DatasetFile;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;
import dataInterface.CompoundPropertyOwner;

public class EmbedUtil
{
	public static double computeRSquare(List<Vector3f> positions, double distanceMatrix[][])
	{
		double d1[][] = distanceMatrix;
		normalizeDistanceMatrix(d1);
		double d2[][] = euclMatrix(positions);
		normalizeDistanceMatrix(d2);
		return computeRSquare(d1, d2);
	}

	public static double computeRSquare(List<Vector3f> positions, List<CompoundPropertyOwner> instances,
			List<CompoundProperty> features, DatasetFile dataset)
	{
		return computeRSquare(positions, euclMatrix(instances, features, dataset));
	}

	private static double computeRSquare(double[][] m1, double[][] m2)
	{
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

	public static double computeCCC(List<Vector3f> positions, double distanceMatrix[][])
	{
		return computeCCC(positions, Dimensions.xyz, distanceMatrix);
	}

	public static double computeCCC(List<Vector3f> positions, Dimensions dims, double distanceMatrix[][])
	{
		double d1[][] = distanceMatrix;
		normalizeDistanceMatrix(d1);
		double d2[][] = euclMatrix(positions, dims);
		normalizeDistanceMatrix(d2);
		return computeCCC(d1, d2);
	}

	public static double computeCCC(List<Vector3f> positions, List<CompoundPropertyOwner> instances,
			List<CompoundProperty> features, DatasetFile dataset)
	{
		return computeCCC(positions, euclMatrix(instances, features, dataset));
	}

	public static double[] computeCCCs(List<Vector3f> positions, double distanceMatrix[][])
	{
		double d1[][] = distanceMatrix;
		normalizeDistanceMatrix(d1);
		double d2[][] = euclMatrix(positions);
		normalizeDistanceMatrix(d2);
		return computeCCCs(d1, d2);
	}

	public static double[] computeCCCs(List<Vector3f> positions, List<CompoundPropertyOwner> instances,
			List<CompoundProperty> features, DatasetFile dataset)
	{
		return computeCCCs(positions, euclMatrix(instances, features, dataset));
	}

	private static double[] computeCCCs(double[][] m1, double[][] m2)
	{
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

	private static double[][] euclMatrix(List<CompoundPropertyOwner> instances, List<CompoundProperty> features,
			DatasetFile dataset)
	{
		HashMap<CompoundPropertyOwner, double[]> vals = new HashMap<CompoundPropertyOwner, double[]>();
		for (int i = 0; i < instances.size(); i++)
		{
			List<Double> v_i = new ArrayList<Double>();
			for (int k = 0; k < features.size(); k++)
			{
				CompoundProperty feature = features.get(k);
				if (feature.getType() == Type.NUMERIC)
				{
					Double v = instances.get(i).getNormalizedValue(feature);
					if (v == null)
						v = feature.getNormalizedMedian(dataset);
					v_i.add(v);
				}
				else
				{

					if (feature.getType() != Type.NOMINAL)
						throw new Error();
					if (feature.getNominalDomain().length == 2 && feature.getNominalDomain()[0].equals("0")
							&& feature.getNominalDomain()[1].equals("1"))
						v_i.add(Double.parseDouble(instances.get(i).getStringValue(features.get(k))));
					else
					{
						for (String val : feature.getNominalDomain())
						{
							if (ObjectUtil.equals(instances.get(i).getStringValue(features.get(k)), val))
								v_i.add(1.0);
							else
								v_i.add(0.0);
						}
					}
				}
			}
			//			Settings.LOGGER.println(vals.size() + " " + ListUtil.toString(v_i));
			vals.put(instances.get(i), ArrayUtil.toPrimitiveDoubleArray(v_i));
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

	public enum Dimensions
	{
		xyz, xy, xz, yz, x, y, z
	}

	private static double[][] euclMatrix(List<Vector3f> positions)
	{
		return euclMatrix(positions, Dimensions.xyz);
	}

	private static double[][] euclMatrix(List<Vector3f> positions, Dimensions dims)
	{
		double[][] d = new double[positions.size()][positions.size()];
		for (int i = 0; i < positions.size() - 1; i++)
			for (int j = i + 1; j < positions.size(); j++)
			{
				d[i][j] = dist(positions.get(i), positions.get(j), dims);
				d[j][i] = d[i][j];
			}
		//		Settings.LOGGER.println(ArrayUtil.toString(d));
		return d;
	}

	private static double dist(Vector3f v1, Vector3f v2, Dimensions dims)
	{
		switch (dims)
		{
			case xyz:
				return Vector3fUtil.dist(v1, v2);
			case xy:
				Vector3f vec1 = new Vector3f(v1);
				Vector3f vec2 = new Vector3f(v2);
				vec1.z = 0;
				vec2.z = 0;
				return Vector3fUtil.dist(vec1, vec2);
			case xz:
				vec1 = new Vector3f(v1);
				vec2 = new Vector3f(v2);
				vec1.y = 0;
				vec2.y = 0;
				return Vector3fUtil.dist(vec1, vec2);
			case yz:
				vec1 = new Vector3f(v1);
				vec2 = new Vector3f(v2);
				vec1.x = 0;
				vec2.x = 0;
				return Vector3fUtil.dist(vec1, vec2);
			case x:
				return Math.abs(v1.x - v2.x);
			case y:
				return Math.abs(v1.y - v2.y);
			case z:
				return Math.abs(v1.z - v2.z);
			default:
				throw new Error("wtf");
		}
	}

	public static void main(String args[])
	{
		Random r = new Random();
		int ns[] = new int[] { 100 }; //{ 10, 50, 100, 500, 1000 };
		int restarts = 30;

		double deviation[] = new double[] { 1, 0.5, 0.25, 0.20, 0.15, 0.10, 0.05 };

		for (int n : ns)
		{
			Settings.LOGGER.info(n);

			for (double dev : deviation)
			{
				Settings.LOGGER.info("dev: " + dev);

				double[] results = new double[restarts];

				for (int k = 0; k < restarts; k++)
				{
					double d[][] = new double[n][n];
					double d2[][] = new double[n][n];

					for (int i = 0; i < n; i++)
						for (int j = 0; j < n; j++)
							if (i != j)
							{
								d[i][j] = r.nextDouble();
								d2[i][j] = d[i][j] + (d[i][j] * dev * (r.nextBoolean() ? 1 : -1));
								//d2[i][j] = r.nextDouble();
							}

					normalizeDistanceMatrix(d);
					normalizeDistanceMatrix(d2);
					results[k] = computeRSquare(d, d2);
				}

				Settings.LOGGER.info(DoubleArraySummary.create(results).getMedian());
			}
			Settings.LOGGER.info();
		}
	}

	//	public static class CompoundPropertyEmbedQuality implements Comparable<CompoundPropertyEmbedQuality>
	//	{
	//		Double d;
	//		CompoundProperty feature;
	//		List<Vector3f> positions;
	//		List<MolecularPropertyOwner> instances;
	//		DatasetFile dataset;
	//
	//		public CompoundPropertyEmbedQuality(CompoundProperty feature, List<Vector3f> positions,
	//				List<MolecularPropertyOwner> instances, DatasetFile dataset)
	//		{
	//			this.feature = feature;
	//			this.positions = positions;
	//			this.instances = instances;
	//			this.dataset = dataset;
	//		}
	//
	//		public CompoundPropertyEmbedQuality clone()
	//		{
	//			return new CompoundPropertyEmbedQuality(feature, positions, instances, dataset);
	//		}
	//
	//		public String toString()
	//		{
	//			if (d == null)
	//				return "computing..";
	//			if (Double.isNaN(d))
	//				return "na";
	//			return StringUtil.formatDouble(d);
	//		}
	//
	//		public double compute(Dimensions dims)
	//		{
	//			if (d == null)
	//			{
	//				if (feature.getType() != Type.NUMERIC && feature.getType() != Type.NOMINAL)
	//					d = Double.NaN;
	//				else
	//				{
	//					List<CompoundProperty> features = new ArrayList<CompoundProperty>();
	//					features.add(feature);
	//					double m[][] = euclMatrix(instances, features, dataset);
	//					d = computeCCC(positions, dims, m);
	//				}
	//			}
	//			return d;
	//		}
	//
	//		public int compareTo(CompoundPropertyEmbedQuality o)
	//		{
	//			if (d == null || Double.isNaN(d))
	//			{
	//				if (o.d == null || Double.isNaN(o.d))
	//					return 0;
	//				return -1;
	//			}
	//			if (o.d == null || Double.isNaN(o.d))
	//				return 1;
	//			return d.compareTo(o.d);
	//		}
	//	}

}
