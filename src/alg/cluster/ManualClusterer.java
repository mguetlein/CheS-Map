package alg.cluster;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Message;
import gui.Messages;
import gui.property.BooleanProperty;
import gui.property.Property;

import java.util.ArrayList;
import java.util.List;

import main.Settings;
import util.ArrayUtil;
import util.IntegerUtil;
import alg.AlgorithmException;
import data.DatasetFile;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;

public class ManualClusterer extends AbstractDatasetClusterer
{
	public static final ManualClusterer INSTANCE = new ManualClusterer();
	private boolean isDisjoint;

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
		return new Property[] { ignoreSingletons };
	}

	@Override
	protected List<Integer[]> cluster(DatasetFile dataset, List<CompoundData> compounds, List<MoleculeProperty> features)
			throws Exception
	{
		MoleculeProperty clusterProp = dataset.getIntegratedClusterProperty();
		if (clusterProp == null)
			throw new AlgorithmException.ClusterException(this, "No feature with name including 'cluster' found");
		List<Integer[]> clusterAssignment = new ArrayList<Integer[]>();
		List<Integer[]> moleculeAssignment = new ArrayList<Integer[]>();

		for (int compoundIndex = 0; compoundIndex < compounds.size(); compoundIndex++)
		{
			CompoundData m = compounds.get(compoundIndex);
			String val = m.getStringValue(clusterProp);
			if (val != null && val.trim().length() > 0)
			{
				String vals[] = val.trim().split(",");
				Integer intVals[] = new Integer[vals.length];
				for (int i = 0; i < intVals.length; i++)
				{
					intVals[i] = IntegerUtil.parseInteger(vals[i]);
					if (intVals[i] == null)
						throw new AlgorithmException.ClusterException(this,
								"Cannot parse cluster assignment as integer: " + vals[i] + " in " + val);
				}
				clusterAssignment.add(intVals);
				for (Integer clazz : intVals)
				{
					while (moleculeAssignment.size() <= clazz)
						moleculeAssignment.add(new Integer[0]);
					moleculeAssignment.set(clazz, ArrayUtil.concat(Integer.class, moleculeAssignment.get(clazz),
							new Integer[] { compoundIndex }));
				}
			}
		}

		if (ignoreSingletons.getValue())
		{
			List<Integer> remClazz = new ArrayList<Integer>();
			for (int c = 0; c < moleculeAssignment.size(); c++)
				if (moleculeAssignment.get(c).length < 2)
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
		//				MoleculeProperty p = clusterProps[clusterPropertyIndex];
		//				for (int compoundIndex = 0; compoundIndex < compounds.size(); compoundIndex++)
		//				{
		//					CompoundData m = compounds.get(compoundIndex);
		//					if (m.getStringValue(p).matches("(?i)(1|true|yes)"))
		//						clusterAssignment.set(compoundIndex, ArrayUtil.concat(clusterAssignment.get(compoundIndex),
		//								new Integer[] { clusterPropertyIndex }));
		//				}
		//			}
		//		}
		isDisjoint = true;
		for (Integer[] clusters : clusterAssignment)
			if (clusters.length > 1)
			{
				isDisjoint = false;
				break;
			}
		return clusterAssignment;
	}

	@Override
	public boolean isDisjointClusterer()
	{
		return isDisjoint;
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
		if (dataset.getIntegratedClusterProperty() == null)
			m.add(Message.errorMessage(Settings.text("cluster.manual.noClusterFeatureFound")));
		else
			m.add(Message.infoMessage(Settings.text("cluster.manual.clusterFeatureFound", dataset
					.getIntegratedClusterProperty().getName())));
		//		else if (dataset.getIntegratedClusterProperties().length > 1)
		//			m.add(Message.infoMessage(Settings.text("cluster.manual.multipleClusterFeatures",
		//					dataset.getIntegratedClusterProperties().length + "")));
		return m;
	}

}
