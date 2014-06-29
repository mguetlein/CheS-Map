package appdomain;

import dataInterface.CompoundProperty;

public class CentroidAppDomainComputer extends DistanceBasedAppDomainComputer
{
	public static CentroidAppDomainComputer INSTANCE = new CentroidAppDomainComputer();

	private CentroidAppDomainComputer()
	{
	}

	@Override
	public double[] computeTrainingDistances()
	{
		double[] trainingDistances = new double[compounds.size()];
		for (int i = 0; i < compounds.size(); i++)
		{
			double dist = 0;
			for (int j = 0; j < features.size(); j++)
			{
				CompoundProperty f = features.get(j);
				dist += Math.pow(
						f.getNormalizedMedianInCompleteDataset() - f.getNormalizedValuesInCompleteDataset()[i], 2);
			}
			trainingDistances[i] = Math.sqrt(dist);
		}
		return trainingDistances;
	}

	@Override
	public String getShortName()
	{
		return "centroid";
	}
}
