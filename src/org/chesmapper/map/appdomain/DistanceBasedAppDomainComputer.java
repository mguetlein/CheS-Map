package org.chesmapper.map.appdomain;

import org.mg.javalib.gui.property.DoubleProperty;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.gui.property.SelectProperty;
import org.mg.javalib.util.DoubleArraySummary;

public abstract class DistanceBasedAppDomainComputer extends AbstractAppDomainComputer
{
	public static enum Method
	{
		median, mean
	}

	protected SelectProperty method;
	//	protected BooleanProperty continousEnabled;
	protected DoubleProperty distanceMultiplier;

	//	protected DoubleProperty fullDistanceMultiplier;

	public DistanceBasedAppDomainComputer()
	{
		String methodStr = "avg compoutation method";
		//		String contStr = "continous enabled";
		String distStr = "distance multiplier";
		//		String fullDistStr = "full distance multiplier";

		method = new SelectProperty(methodStr, methodStr + "." + getShortName(), Method.values(), Method.mean);
		//		continousEnabled = new BooleanProperty(contStr, contStr + "." + getShortName(), true);
		distanceMultiplier = new DoubleProperty(distStr, distStr + "." + getShortName(), 3.0);
		//		fullDistanceMultiplier = new DoubleProperty(fullDistStr, fullDistStr + "." + getShortName(), 1.0);
	}

	public Property[] getProperties()
	{
		return new Property[] { distanceMultiplier, method, //continousEnabled, 
		//				fullDistanceMultiplier
		};
	}

	public abstract double[] computeTrainingDistances();

	//	double maxTrainingDistance;
	double avgTrainingDistance;

	//	public double getApplicabilityDomainDistance()
	//	{
	//		return Math.min(maxTrainingDistance, avgTrainingDistance * distanceMultiplier.getValue());
	//	}

	//	public double getContinousFullApplicabilityDomainDistance()
	//	{
	//		return avgTrainingDistance * fullDistanceMultiplier.getValue();
	//	}

	@Override
	public void computeAppDomain()
	{
		double[] distances = computeTrainingDistances();

		DoubleArraySummary sum = DoubleArraySummary.create(distances);
		//		maxTrainingDistance = sum.getMax();
		if (method.getValue() == Method.mean)
			avgTrainingDistance = sum.getMean();
		else
			avgTrainingDistance = sum.getMedian();

		for (int i = 0; i < pValues.length; i++)
		{
			pValues[i] = distances[i] / avgTrainingDistance;
			inside[i] = pValues[i] < distanceMultiplier.getValue();
		}

	}

	//	public boolean getApplicabilityDomainPropability(Double x)
	//	{
	//		if (x < 0)
	//			throw new Error();
	//		if (x > getApplicabilityDomainDistance())
	//			return false;
	//		if (x < getContinousFullApplicabilityDomainDistance())
	//			return 1.0;
	//		//map fullAd-ad to -3, 3
	//		x -= getContinousFullApplicabilityDomainDistance();
	//		x /= (getApplicabilityDomainDistance() - getContinousFullApplicabilityDomainDistance());
	//		x *= 6;
	//		x -= 3;
	//		double y = Math.tanh(x);
	//		// put upside down
	//		y *= -1;
	//		// transition from -1 - 1 to 0-1
	//		y += 1;
	//		y /= 2.0;
	//		return y;
	//	}

}
