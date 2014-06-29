package alg.cluster;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Message;
import gui.Messages;
import gui.property.BooleanProperty;
import gui.property.Property;
import gui.property.SelectProperty;

import java.util.ArrayList;
import java.util.List;

import main.Settings;
import util.ArrayUtil;
import alg.AlgorithmException;
import alg.DistanceMeasure;
import data.DatasetFile;
import data.integrated.IntegratedPropertySet;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;

public class ManualClusterer extends AbstractDatasetClusterer
{
	public static final ManualClusterer INSTANCE = new ManualClusterer();

	SelectProperty clusterFeature = new SelectProperty("Cluster feature", null, null);
	BooleanProperty ignoreSingletons = new BooleanProperty("Ignore singelton clusters", false);

	public ManualClusterer()
	{
		clusterApproach = ClusterApproach.Other;
	}

	@Override
	public String getName()
	{
		return Settings.text("cluster.manual");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("cluster.manual.desc");
	}

	@Override
	public Property[] getProperties()
	{
		return new Property[] { clusterFeature, ignoreSingletons };
	}

	@Override
	public void update(DatasetFile dataset)
	{
		if (dataset.getIntegratedProperties().length > 0)
			clusterFeature.reset(ArrayUtil.toStringArray(dataset.getIntegratedProperties()));
		else
			clusterFeature.reset(null);
	}

	@Override
	protected List<Integer[]> cluster(DatasetFile dataset, List<CompoundData> compounds, List<CompoundProperty> features)
			throws Exception
	{
		CompoundProperty clusterProp = null;
		for (IntegratedPropertySet c : dataset.getIntegratedProperties())
			if (c.toString().equals(clusterFeature.getValue().toString()))
			{
				clusterProp = c.get();
				break;
			}
		if (clusterProp == null)
			throw new AlgorithmException.ClusterException(this, "No internal feature selected");
		List<Integer[]> clusterAssignment = new ArrayList<Integer[]>();
		List<Integer[]> compoundAssignment = new ArrayList<Integer[]>();

		try
		{
			for (int compoundIndex = 0; compoundIndex < compounds.size(); compoundIndex++)
			{
				CompoundData m = compounds.get(compoundIndex);

				Integer intVals[] = new Integer[0];
				if (clusterProp.getType() == Type.NUMERIC && clusterProp.isInteger())
				{
					if (m.getDoubleValue(clusterProp) != null)
						intVals = new Integer[] { m.getDoubleValue(clusterProp).intValue() };
				}
				else
				{
					String val = m.getStringValue(clusterProp);
					if (val != null && val.trim().length() > 0)
					{
						String vals[] = val.trim().split(",");
						intVals = new Integer[vals.length];
						for (int i = 0; i < intVals.length; i++)
						{
							intVals[i] = Integer.parseInt(vals[i]);
						}
					}
				}
				clusterAssignment.add(intVals);
				if (intVals.length > 0)
				{
					for (Integer clus : intVals)
					{
						while (compoundAssignment.size() <= clus)
							compoundAssignment.add(new Integer[0]);
						compoundAssignment.set(clus, ArrayUtil.concat(Integer.class, compoundAssignment.get(clus),
								new Integer[] { compoundIndex }));
					}
				}
			}
		}
		catch (NumberFormatException e)
		{
			String domain[] = clusterProp.getNominalDomain();

			for (int compoundIndex = 0; compoundIndex < compounds.size(); compoundIndex++)
			{
				CompoundData m = compounds.get(compoundIndex);
				String val = m.getStringValue(clusterProp);
				if (val == null)
					clusterAssignment.add(new Integer[0]);
				else
				{
					int i = ArrayUtil.indexOf(domain, val);
					if (i == -1)
						throw new Error("should never happen");
					clusterAssignment.add(new Integer[] { i });
					while (compoundAssignment.size() <= i)
						compoundAssignment.add(new Integer[0]);
					compoundAssignment
							.set(i, ArrayUtil.concat(Integer.class, compoundAssignment.get(i),
									new Integer[] { compoundIndex }));
				}
			}
		}

		if (ignoreSingletons.getValue())
		{
			List<Integer> remClazz = new ArrayList<Integer>();
			for (int c = 0; c < compoundAssignment.size(); c++)
				if (compoundAssignment.get(c).length < 2)
					remClazz.add(c);
			System.out.println("removing " + remClazz.size() + " singleton clusters");
			if (remClazz.size() > 0)
			{
				List<Integer[]> newClusterAssignment = new ArrayList<Integer[]>();
				for (int m = 0; m < clusterAssignment.size(); m++)
				{
					List<Integer> newClusters = new ArrayList<Integer>();
					for (int c : clusterAssignment.get(m))
						if (!remClazz.contains(new Integer(c)))
							newClusters.add(c);
					if (newClusters.size() == 0)
						newClusterAssignment.add(new Integer[0]);
					else
						newClusterAssignment.add(ArrayUtil.toArray(newClusters));
				}
				clusterAssignment = newClusterAssignment;
			}
		}
		//		else
		//		{
		//			for (int compoundIndex = 0; compoundIndex < compounds.size(); compoundIndex++)
		//				clusterAssignment.add(new Integer[0]);
		//			for (int clusterPropertyIndex = 0; clusterPropertyIndex < clusterProps.length; clusterPropertyIndex++)
		//			{
		//				CompoundProperty p = clusterProps[clusterPropertyIndex];
		//				for (int compoundIndex = 0; compoundIndex < compounds.size(); compoundIndex++)
		//				{
		//					CompoundData m = compounds.get(compoundIndex);
		//					if (m.getStringValue(p).matches("(?i)(1|true|yes)"))
		//						clusterAssignment.set(compoundIndex, ArrayUtil.concat(clusterAssignment.get(compoundIndex),
		//								new Integer[] { clusterPropertyIndex }));
		//				}
		//			}
		//		}
		return clusterAssignment;
	}

	@Override
	protected String getShortName()
	{
		return "manuClust";
	}

	@Override
	public boolean requiresFeatures()
	{
		return false;
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		if (clusterFeature.getValue().toString().equals(SelectProperty.EMPTY))
			m.add(Message.errorMessage(Settings.text("cluster.manual.noIntegratedProperties")));
		//		else
		//			m.add(Message.infoMessage(Settings.text("cluster.manual.clusterFeatureSelected", clusterFeature.getValue()
		//					.toString())));
		//		else if (dataset.getIntegratedClusterProperties().length > 1)
		//			m.add(Message.infoMessage(Settings.text("cluster.manual.multipleClusterFeatures",
		//					dataset.getIntegratedClusterProperties().length + "")));
		return m;
	}

	@Override
	public DistanceMeasure getDistanceMeasure()
	{
		return DistanceMeasure.UNKNOWN_DISTANCE;
	}

}
