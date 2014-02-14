package weka;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import util.ArrayUtil;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.output.prediction.AbstractOutput;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;

public class Predictor
{
	public static double[] predict(List<CompoundData> compounds, List<CompoundProperty> features, CompoundProperty clazz)
	{
		try
		{
			if (clazz.getType() != Type.NOMINAL && clazz.getNominalDomainInMappedDataset().length != 2)
				throw new Error();
			final boolean swapPredictedDouble = clazz.getNominalDomainInMappedDataset()[0].equals("active");

			List<CompoundProperty> p = new ArrayList<CompoundProperty>(features);
			p.add(clazz);
			if (new File("/tmp/arff-file.arff").exists())
				new File("/tmp/arff-file.arff").delete();
			File arffFile = CompoundArffWriter.writeArffFile("/tmp/arff-file.arff", compounds, p);
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
			double acc[] = new double[rep];
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
							predictions[w[i]][rep_i] = swapPredictedDouble ? (1 - d[i]) : d[i];
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
						m_NumFolds = numFolds;

						if (classificationOutput != null)
							classificationOutput.printFooter();
					}
				};
				eval.crossValidateModel(new RandomForest(), data, 10, new Random(i));
				System.out.println(eval.pctCorrect());
				acc[i] = eval.pctCorrect();
			}

			System.out.println("mean acc: " + ArrayUtil.getMean(acc));

			double d[] = new double[compounds.size()];
			for (int i = 0; i < compounds.size(); i++)
			{
				double pred[] = predictions[i];
				//				System.out.println(compounds.get(i).getFormattedValue(clazz) + " "
				//						+ data.get(i).stringValue(data.numAttributes() - 1) + " " + ArrayUtil.toString(pred));
				d[i] = ArrayUtil.getMean(pred);
			}
			return d;

		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
