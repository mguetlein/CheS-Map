package data;

import java.util.ArrayList;
import java.util.List;

import main.TaskProvider;
import alg.FeatureComputer;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import dataInterface.MoleculePropertySet;

public class DefaultFeatureComputer implements FeatureComputer
{
	MoleculePropertySet moleculePropertySets[];

	List<MoleculeProperty> features;
	List<MoleculeProperty> properties;
	List<CompoundData> compounds;

	public DefaultFeatureComputer()
	{
		this(null);
	}

	public DefaultFeatureComputer(MoleculePropertySet moleculePropertySets[])
	{
		this.moleculePropertySets = moleculePropertySets;
	}

	@Override
	public void computeFeatures(DatasetFile dataset)
	{
		features = new ArrayList<MoleculeProperty>();
		properties = new ArrayList<MoleculeProperty>();
		compounds = new ArrayList<CompoundData>();

		int numCompounds = dataset.numCompounds();

		List<MoleculeProperty> props = new ArrayList<MoleculeProperty>();

		int count = 0;
		for (MoleculePropertySet propSet : moleculePropertySets)
		{
			TaskProvider.task()
					.update("Computing feature " + (count + 1) + "/" + moleculePropertySets.length + " : "
							+ propSet.toString());

			if (propSet instanceof IntegratedProperty)
				((IntegratedProperty) propSet).setUsedForMapping(true);

			if (!propSet.isComputed(dataset))
				propSet.compute(dataset);

			for (int i = 0; i < propSet.getSize(dataset); i++)
			{
				props.add(propSet.get(dataset, i));
				features.add(propSet.get(dataset, i));
			}
			count++;
		}

		for (IntegratedProperty p : dataset.getIntegratedProperties(false))
			if (!props.contains(p))
			{
				p.setUsedForMapping(false);
				props.add(p);
				properties.add(p);
			}

		String[] smiles = dataset.getSmiles();

		for (MoleculeProperty p : props)
		{
			Double d[] = null;
			String s[] = null;
			if (p.getType() == Type.NUMERIC)
				d = p.getDoubleValues(dataset);
			else
				s = p.getStringValues(dataset);
			Double n[] = p.getNormalizedValues(dataset);

			if ((d != null && d.length != numCompounds) || (s != null && s.length != numCompounds))
				throw new Error("illegal num features " + p);

			if (TaskProvider.task().isCancelled())
				break;

			for (int i = 0; i < numCompounds; i++)
			{
				CompoundDataImpl c;
				if (compounds.size() > i)
					c = (CompoundDataImpl) compounds.get(i);
				else
				{
					c = new CompoundDataImpl(smiles[i]);
					c.setIndex(i);
					compounds.add(c);
				}

				if (p.getType() == Type.NUMERIC)
					c.setDoubleValue(p, d[i]);
				else
					c.setStringValue(p, s[i]);
				c.setNormalizedValue(p, n[i]);
			}
		}
	}

	@Override
	public List<MoleculeProperty> getFeatures()
	{
		return features;
	}

	@Override
	public List<MoleculeProperty> getProperties()
	{
		return properties;
	}

	@Override
	public List<CompoundData> getCompounds()
	{
		return compounds;
	}

}
