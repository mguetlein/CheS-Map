package data;

import gui.Progressable;

import java.util.ArrayList;
import java.util.List;

import main.Settings;
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
	public void computeFeatures(DatasetFile dataset, Progressable progress)
	{
		features = new ArrayList<MoleculeProperty>();
		properties = new ArrayList<MoleculeProperty>();
		compounds = new ArrayList<CompoundData>();

		int numCompounds = dataset.numCompounds();

		List<MoleculeProperty> props = new ArrayList<MoleculeProperty>();

		for (MoleculePropertySet propSet : moleculePropertySets)
		{
			for (int i = 0; i < propSet.getSize(); i++)
			{
				props.add(propSet.get(i));
				features.add(propSet.get(i));
			}
		}

		for (IntegratedProperty p : dataset.getIntegratedProperties(false))
			if (!props.contains(p))
			{
				props.add(p);
				properties.add(p);
			}

		System.out.print("computing features ");
		String[] smiles = dataset.getSmiles();

		int count = 0;
		for (MoleculeProperty p : props)
		{
			System.out.print(".");
			if (progress != null)
				progress.update((100 / (double) props.size()) * count,
						"computing feature " + (count + 1) + "/" + props.size() + " : " + p.toString());
			count++;

			Double d[] = null;
			String s[] = null;
			if (p.getType() == Type.NUMERIC)
				d = dataset.getDoubleValues(p);
			else
				s = dataset.getStringValues(p);
			Double n[] = dataset.getNormalizedValues(p);

			if (Settings.isAborted(Thread.currentThread()))
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
		System.out.println(" done");
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
