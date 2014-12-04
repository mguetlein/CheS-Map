package org.chesmapper.map.appdomain;

import java.util.Arrays;

import org.chesmapper.map.alg.embed3d.EmbedUtil;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.mg.javalib.gui.property.IntegerProperty;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.DoubleArraySummary;
import org.mg.javalib.util.ListUtil;

public class KNNAppDomainComputer extends DistanceBasedAppDomainComputer
{
	public static KNNAppDomainComputer INSTANCE = new KNNAppDomainComputer();

	private KNNAppDomainComputer()
	{
	}

	IntegerProperty numNeigbors = new IntegerProperty("Num neighbors", 3);

	public Property[] getProperties()
	{
		return ArrayUtil.concat(Property.class, new Property[] { numNeigbors }, super.getProperties());
	}

	@Override
	public double[] computeTrainingDistances()
	{
		double[][] featureDistanceMatrix = EmbedUtil.euclMatrix(compounds,
				ListUtil.cast(CompoundProperty.class, features));
		double[] trainingDistances = new double[compounds.size()];
		for (int i = 0; i < compounds.size(); i++)
		{
			double[] dist = new double[compounds.size() - 1];
			int count = 0;
			for (int j = 0; j < compounds.size(); j++)
				if (i != j)
					dist[count++] = featureDistanceMatrix[i][j];
			Arrays.sort(dist);
			dist = Arrays.copyOfRange(dist, 0, numNeigbors.getValue());
			//			if (method == Method.median)
			//				trainingDistances[i] = DoubleArraySummary.create(dist).getMedian();
			//			else
			trainingDistances[i] = DoubleArraySummary.create(dist).getMean();
		}
		return trainingDistances;
	}

	@Override
	public String getShortName()
	{
		return "knn";
	}

}
