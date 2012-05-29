package alg.embed3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.vecmath.Vector3f;

import main.Settings;
import util.ArrayUtil;
import util.DoubleArraySummary;
import util.Vector3fUtil;
import data.DatasetFile;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;

public class EmbedUtil
{
	public static double computeRSquare(List<MolecularPropertyOwner> instances, List<MoleculeProperty> features,
			List<Vector3f> positions, DatasetFile dataset)
	{
		double d1[][] = euclMatrix(instances, features, dataset);
		double d2[][] = euclMatrix(positions);
		return computeRSquare(d1, d2);
	}

	private static double computeRSquare(double[][] featureDistances, double[][] threeDDistances)
	{
		double ss_err = 0;
		double ss_tot = 0;
		double featureDistances_mean = 0;
		int count = 0;
		for (int i = 0; i < featureDistances.length - 1; i++)
			for (int j = i + 1; j < featureDistances.length; j++)
			{
				featureDistances_mean += featureDistances[i][j];
				count += 1;
			}
		featureDistances_mean /= (double) count;
		for (int i = 0; i < featureDistances.length - 1; i++)
			for (int j = i + 1; j < featureDistances.length; j++)
			{
				ss_err += Math.pow(featureDistances[i][j] - threeDDistances[i][j], 2);
				ss_tot += Math.pow(featureDistances[i][j] - featureDistances_mean, 2);
			}
		return 1 - ss_err / ss_tot;
	}

	private static double[][] euclMatrix(List<MolecularPropertyOwner> instances, List<MoleculeProperty> features,
			DatasetFile dataset)
	{
		HashMap<MolecularPropertyOwner, double[]> vals = new HashMap<MolecularPropertyOwner, double[]>();
		for (int i = 0; i < instances.size(); i++)
		{
			List<Double> v_i = new ArrayList<Double>();
			for (int k = 0; k < features.size(); k++)
			{
				MoleculeProperty feature = features.get(k);
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
							if (instances.get(i).getStringValue(features.get(k)).equals(val))
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
				d[i][j] = ArrayUtil.euclDistance(vals.get(instances.get(i)), vals.get(instances.get(j)));

		//hack to make normalization work
		for (int i = 0; i < instances.size(); i++)
			for (int j = i; j < instances.size(); j++)
				d[j][i] = d[0][1];
		ArrayUtil.normalize(d);
		for (int i = 0; i < instances.size(); i++)
			for (int j = i; j < instances.size(); j++)
				d[j][i] = 0.0;
		//		Settings.LOGGER.println(ArrayUtil.toString(d));

		return d;
	}

	private static double[][] euclMatrix(List<Vector3f> positions)
	{
		double[][] d = new double[positions.size()][positions.size()];
		for (int i = 0; i < positions.size() - 1; i++)
			for (int j = i + 1; j < positions.size(); j++)
				d[i][j] = (double) Vector3fUtil.dist(positions.get(i), positions.get(j));

		//hack to make normalization work
		for (int i = 0; i < positions.size(); i++)
			for (int j = i; j < positions.size(); j++)
				d[j][i] = d[0][1];
		ArrayUtil.normalize(d);
		for (int i = 0; i < positions.size(); i++)
			for (int j = i; j < positions.size(); j++)
				d[j][i] = 0.0;
		//		Settings.LOGGER.println(ArrayUtil.toString(d));

		return d;
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
				Settings.LOGGER.info(dev);

				double[] results = new double[restarts];

				for (int k = 0; k < restarts; k++)
				{
					double d[][] = new double[n][n];
					double d2[][] = new double[n][n];

					for (int i = 0; i < n - 1; i++)
						for (int j = i + 1; j < n; j++)
						{
							d[i][j] = r.nextDouble();

							d2[i][j] = d[i][j] + (d[i][j] * dev * (r.nextBoolean() ? 1 : -1));

							//d2[i][j] = r.nextDouble();
						}

					//hack to make normalization work
					for (int i = 0; i < n; i++)
						for (int j = i; j < n; j++)
							d[j][i] = d[0][1];
					ArrayUtil.normalize(d);
					for (int i = 0; i < n; i++)
						for (int j = i; j < n; j++)
							d[j][i] = 0.0;
					//		Settings.LOGGER.println(ArrayUtil.toString(d));

					//hack to make normalization work
					for (int i = 0; i < n; i++)
						for (int j = i; j < n; j++)
							d2[j][i] = d2[0][1];
					ArrayUtil.normalize(d2);
					for (int i = 0; i < n; i++)
						for (int j = i; j < n; j++)
							d2[j][i] = 0.0;
					//		Settings.LOGGER.println(ArrayUtil.toString(d));

					results[k] = computeRSquare(d, d2);
				}

				Settings.LOGGER.info(DoubleArraySummary.create(results).getMedian());
			}
			Settings.LOGGER.info();
		}

	}

}
