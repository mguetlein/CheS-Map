package data;

import java.util.ArrayList;
import java.util.List;

import util.ArrayUtil;
import alg.FeatureComputer;
import data.CDKProperty.CDKDescriptor;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;

public class CDKFeatureComputer implements FeatureComputer
{
	IntegratedProperty integratedFeatures[];
	data.CDKProperty.CDKDescriptor cdkDescriptors[];

	List<MoleculeProperty> features;
	List<MoleculeProperty> properties;
	List<CompoundData> compounds;

	public CDKFeatureComputer()
	{
		this(null, new CDKDescriptor[] { CDKDescriptor.Weight, CDKDescriptor.XLogP });
		//private static Property FEATURES[] = {};
		//		CDKService.CDK_NUMERIC_FEATURES;
		//	{ CDKProperty.Weight, CDKProperty.XLogP, CDKProperty.HBondAcceptorCount,
		//			CDKProperty.ALOGP, CDKProperty.ChiChain, CDKProperty.BondCount, CDKProperty.PetitjeanNumber }; 
		//{ CDKProperty.HBondAcceptorCount, CDKProperty.HBondDonorCount, CDKProperty.Weight,CDKProperty.ALOGP };
	}

	public CDKFeatureComputer(IntegratedProperty integratedFeatures[], CDKDescriptor cdkDescriptors[])
	{
		this.integratedFeatures = integratedFeatures;
		this.cdkDescriptors = cdkDescriptors;
	}

	@Override
	public void computeFeatures(DatasetFile dataset)
	{
		features = new ArrayList<MoleculeProperty>();
		properties = new ArrayList<MoleculeProperty>();
		compounds = new ArrayList<CompoundData>();

		int numCompounds = dataset.numCompounds();

		List<MoleculeProperty> props = new ArrayList<MoleculeProperty>();
		for (IntegratedProperty p : integratedFeatures)
		{
			props.add(p);
			features.add(p);
		}
		if (cdkDescriptors != null)
			for (CDKDescriptor p : cdkDescriptors)
				for (int i = 0; i < CDKProperty.numFeatureValues(p); i++)
				{
					CDKProperty pp = new CDKProperty(p, i);
					props.add(pp);
					features.add(pp);
				}
		for (IntegratedProperty p : dataset.getIntegratedProperties(false))
			if (ArrayUtil.indexOf(integratedFeatures, p) == -1)
			{
				props.add(p);
				properties.add(p);
			}

		System.out.print("computing features ");
		String[] smiles = dataset.getSmiles();
		for (MoleculeProperty p : props)
		{
			System.out.print(".");
			Object o[] = dataset.getObjectValues(p);
			Object o2[] = dataset.getDoubleValues(p, true);

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
				c.setValue(p, o[i], false);
				c.setValue(p, o2[i], true);
			}
		}
		System.out.println(" done");
		//compoundFeatures.get(i)[c] = values[i] == null ? Double.NaN : (Double) values[i];
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
