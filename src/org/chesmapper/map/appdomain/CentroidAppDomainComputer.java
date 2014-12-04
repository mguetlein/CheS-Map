package org.chesmapper.map.appdomain;

import org.chesmapper.map.dataInterface.NumericProperty;

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
				NumericProperty f = features.get(j);
				dist += Math.pow(
						f.getNormalizedMedian() - f.getNormalizedValues()[i], 2);
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
