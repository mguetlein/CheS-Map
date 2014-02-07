package data;

import java.util.ArrayList;
import java.util.List;

import main.TaskProvider;
import alg.FeatureComputer;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;
import dataInterface.CompoundPropertySet;

public class DefaultFeatureComputer implements FeatureComputer
{
	CompoundPropertySet compoundPropertySets[];

	List<CompoundProperty> features;
	List<CompoundProperty> properties;
	List<CompoundData> compounds;

	public DefaultFeatureComputer()
	{
		this(null);
	}

	public DefaultFeatureComputer(CompoundPropertySet compoundPropertySets[])
	{
		this.compoundPropertySets = compoundPropertySets;
	}

	@Override
	public void computeFeatures(DatasetFile dataset)
	{
		features = new ArrayList<CompoundProperty>();
		properties = new ArrayList<CompoundProperty>();
		compounds = new ArrayList<CompoundData>();

		int numCompounds = dataset.numCompounds();
		List<CompoundProperty> props = new ArrayList<CompoundProperty>();

		int count = 0;
		for (CompoundPropertySet propSet : compoundPropertySets)
		{
			TaskProvider.update("Compute feature " + (count + 1) + "/" + compoundPropertySets.length + " : "
					+ propSet.toString());

			if (propSet instanceof IntegratedProperty)
				((IntegratedProperty) propSet).setUsedForMapping(true);

			boolean computed = true;
			if (!propSet.isComputed(dataset))
				computed = propSet.compute(dataset);

			if (computed)
			{
				for (int i = 0; i < propSet.getSize(dataset); i++)
				{
					props.add(propSet.get(dataset, i));
					features.add(propSet.get(dataset, i));

					if (!TaskProvider.isRunning())
						return;
				}
			}
			if (!TaskProvider.isRunning())
				return;
			count++;
		}

		for (IntegratedProperty p : dataset.getIntegratedProperties())
			if (!props.contains(p))
			{
				p.setUsedForMapping(false);
				props.add(p);
				properties.add(p);
			}

		String[] smiles = dataset.getSmiles();

		for (int i = 0; i < numCompounds; i++)
		{
			CompoundDataImpl c = new CompoundDataImpl(smiles[i], dataset.getCompounds()[i]);
			c.setOrigIndex(i);
			compounds.add(c);
		}
		for (CompoundProperty p : props)
		{
			Double d[] = null;
			String s[] = null;
			Double n[] = null;
			if (p.getType() == Type.NUMERIC)
			{
				d = p.getDoubleValues(dataset);
				n = p.getNormalizedValues(dataset);
			}
			else
				s = p.getStringValues(dataset);

			if ((d != null && d.length != numCompounds) || (s != null && s.length != numCompounds))
				throw new Error("illegal num features " + p + ", is:" + (d != null ? d.length : s.length)
						+ ", should be:" + numCompounds);

			if (!TaskProvider.isRunning())
				return;

			for (int i = 0; i < numCompounds; i++)
			{
				CompoundDataImpl c = (CompoundDataImpl) compounds.get(i);

				if (p.getType() == Type.NUMERIC)
				{
					c.setDoubleValue(p, d[i]);
					c.setNormalizedValueCompleteDataset(p, n[i]);
				}
				else
					c.setStringValue(p, s[i]);
			}
		}
	}

	@Override
	public List<CompoundProperty> getFeatures()
	{
		return features;
	}

	@Override
	public List<CompoundProperty> getProperties()
	{
		return properties;
	}

	@Override
	public List<CompoundData> getCompounds()
	{
		return compounds;
	}

}
