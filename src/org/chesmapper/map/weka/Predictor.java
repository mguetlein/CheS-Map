package org.chesmapper.map.weka;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.DefaultNumericProperty;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.mg.javalib.util.ArrayUtil;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.output.prediction.AbstractOutput;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.converters.ArffLoader;

public class Predictor
{
	public static class PredictionResult
	{
		boolean classification;
		double[] prediction;
		double[] missClassfied;

		public PredictionResult(double[] prediction, double[] missClassfied, boolean classification)
		{
			this.classification = classification;
			this.prediction = prediction;
			this.missClassfied = missClassfied;
		}

		public NumericProperty createFeature()
		{
			return new DefaultNumericProperty("prediction", ".", ArrayUtil.toDoubleArray(prediction));
		}

		public NumericProperty createMissclassifiedFeature()
		{
			return new DefaultNumericProperty(classification ? "miss-classified" : "error", ".",
					ArrayUtil.toDoubleArray(missClassfied));
		}
	}

	public static PredictionResult predict(List<CompoundData> compounds, List<CompoundProperty> features,
			CompoundProperty clazz, final boolean classification)
	{
		File arffFile = null;
		try
		{
			if (classification)
			{
				if (clazz instanceof NominalProperty && ((NominalProperty) clazz).getDomain().length != 2)
					throw new Error();
			}
			final boolean swapPredictedDouble = classification
					&& ((NominalProperty) clazz).getDomain()[0].equals("active");

			List<CompoundProperty> p = new ArrayList<CompoundProperty>(features);
			p.add(clazz);
			String tmpPath = File.createTempFile("data", "arff").getAbsolutePath();
			if (new File(tmpPath).exists())
				new File(tmpPath).delete();
			arffFile = CompoundArffWriter.writeArffFile(tmpPath, compounds, p);
			ArffLoader loader = new ArffLoader();
			loader.setFile(arffFile);
			Instances data = loader.getDataSet();
			data.setClassIndex(data.numAttributes() - 1);
			System.out.println(data.numInstances() + " " + data.numAttributes());

			int rep = 10;
			final double[][] predictions = new double[data.numInstances()][rep];
			for (int i = 0; i < predictions.length; i++)
				for (int j = 0; j < predictions[0].length; j++)
					predictions[i][j] = Double.MAX_VALUE;
			double performance[] = new double[rep];
			for (int i = 0; i < rep; i++)
			{
				final int rep_i = i;
				Evaluation eval = new Evaluation(data)
				{
					@Override
					public double[] evaluateModel(Classifier classifier, Instances data,
							Object... forPredictionsPrinting) throws Exception
					{
						int[] w = new int[data.numInstances()];
						for (int i = 0; i < data.numInstances(); i++)
						{
							w[i] = (int) data.get(i).weight();
							data.get(i).setWeight(1.0);
						}
						double[] d = super.evaluateModel(classifier, data, forPredictionsPrinting);
						for (int i = 0; i < data.numInstances(); i++)
						{
							if (classification)
								predictions[w[i]][rep_i] = swapPredictedDouble ? (1 - d[i]) : d[i];
							else
								predictions[w[i]][rep_i] = d[i];
						}
						return d;
					}

					public void crossValidateModel(Classifier classifier, Instances data, int numFolds, Random random,
							Object... forPredictionsPrinting) throws Exception
					{

						// Make a copy of the data we can reorder
						data = new Instances(data);
						int count = 0;
						for (Instance i : data)
							i.setWeight(count++);
						data.randomize(random);
						if (data.classAttribute().isNominal())
						{
							data.stratify(numFolds);
						}

						// We assume that the first element is a
						// weka.classifiers.evaluation.output.prediction.AbstractOutput object
						AbstractOutput classificationOutput = null;
						if (forPredictionsPrinting.length > 0)
						{
							// print the header first
							classificationOutput = (AbstractOutput) forPredictionsPrinting[0];
							classificationOutput.setHeader(data);
							classificationOutput.printHeader();
						}

						// Do the folds
						for (int i = 0; i < numFolds; i++)
						{
							Instances train = data.trainCV(numFolds, i, random);
							setPriors(train);
							Classifier copiedClassifier = AbstractClassifier.makeCopy(classifier);
							for (int j = 0; j < train.numInstances(); j++)
								train.get(j).setWeight(1.0);
							copiedClassifier.buildClassifier(train);
							Instances test = data.testCV(numFolds, i);
							evaluateModel(copiedClassifier, test, forPredictionsPrinting);
						}
						//m_NumFolds = numFolds;

						if (classificationOutput != null)
							classificationOutput.printFooter();
					}
				};
				Classifier classifier;
				if (classification)
					classifier = new RandomForest();
				else
				{
					classifier = new SMOreg();
					((SMOreg) classifier).setFilterType(new SelectedTag(SMOreg.FILTER_NONE, SMOreg.TAGS_FILTER));
				}
				eval.crossValidateModel(classifier, data, 10, new Random(i));
				if (classification)
				{
					System.out.println(eval.pctCorrect());
					performance[i] = eval.pctCorrect();
				}
				else
				{
					System.out.println(eval.meanAbsoluteError());
					performance[i] = eval.meanAbsoluteError();
				}
			}

			System.out.println("mean " + (classification ? "acc" : "mean-abs-error") + ": "
					+ ArrayUtil.getMean(performance));

			double predicted[] = new double[compounds.size()];
			for (int i = 0; i < compounds.size(); i++)
			{
				double pred[] = predictions[i];
				//				System.out.println(compounds.get(i).getFormattedValue(clazz) + " "
				//						+ data.get(i).stringValue(data.numAttributes() - 1) + " " + ArrayUtil.toString(pred));
				predicted[i] = ArrayUtil.getMean(pred);
			}

			double error[] = new double[predicted.length];
			for (int i = 0; i < error.length; i++)
			{
				if (classification)
				{
					double actual = ArrayUtil.indexOf(((NominalProperty) clazz).getDomain(), compounds.get(i)
							.getStringValue((NominalProperty) clazz));
					if (actual == 0)
						error[i] = 1 - predicted[i];
					else
						error[i] = predicted[i];
				}
				else
				{
					double actual = compounds.get(i).getDoubleValue((NumericProperty) clazz);
					double sum = 0;
					for (double d : predictions[i])
						sum += Math.abs(actual - d);
					error[i] = sum / (double) rep;
				}
			}
			return new PredictionResult(predicted, error, classification);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			if (arffFile != null && arffFile.exists())
				arffFile.delete();
		}
		return null;
	}
}
