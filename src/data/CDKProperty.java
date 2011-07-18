package data;

import org.openscience.cdk.qsar.IMolecularDescriptor;

import dataInterface.MoleculeProperty;

public class CDKProperty implements MoleculeProperty
{
	public static enum CDKDescriptor
	{
		SMILES, ALOGP, AminoAcidCount, APol, AromaticAtomsCount, AromaticBondsCount, AtomCount,
		AutocorrelationDescriptorCharge, AutocorrelationDescriptorMass, AutocorrelationDescriptorPolarizability, BCUT,
		BondCount, BPol, CarbonTypes, ChiChain, ChiCluster, ChiPathCluster, ChiPath, CPSA, EccentricConnectivityIndex,
		FragmentComplexity, GravitationalIndex, HBondAcceptorCount, HBondDonorCount, IPMolecularLearning,
		KappaShapeIndices, KierHallSmarts, LargestChain, LargestPiSystem, LengthOverBreadth, LongestAliphaticChain,
		MDE, MomentOfInertia, PetitjeanNumber, PetitjeanShapeIndex, RotatableBondsCount, RuleOfFive, TPSA, VAdjMa,
		Weight, WeightedPath, WHIM, WienerNumbers, XLogP, ZagrebIndex
	};

	public static CDKDescriptor[] CDK_NUMERIC_DESCRIPTORS = { //CDKProperty.ALOGP, 
	CDKDescriptor.APol, CDKDescriptor.AminoAcidCount, CDKDescriptor.AromaticAtomsCount,
			CDKDescriptor.AromaticBondsCount, CDKDescriptor.AtomCount, CDKDescriptor.AutocorrelationDescriptorCharge,
			CDKDescriptor.AutocorrelationDescriptorMass, CDKDescriptor.AutocorrelationDescriptorPolarizability,
			CDKDescriptor.BCUT, CDKDescriptor.BondCount, CDKDescriptor.BPol, CDKDescriptor.CarbonTypes,
			CDKDescriptor.ChiChain, CDKDescriptor.ChiCluster, CDKDescriptor.ChiPathCluster, CDKDescriptor.ChiPath,
			CDKDescriptor.CPSA,
			CDKDescriptor.EccentricConnectivityIndex,
			CDKDescriptor.FragmentComplexity,
			CDKDescriptor.GravitationalIndex,
			CDKDescriptor.HBondAcceptorCount,
			CDKDescriptor.HBondDonorCount,
			//			CDKDescriptor.IPMolecularLearning,
			CDKDescriptor.KappaShapeIndices, CDKDescriptor.KierHallSmarts, CDKDescriptor.LargestChain,
			CDKDescriptor.LargestPiSystem, CDKDescriptor.LengthOverBreadth, CDKDescriptor.LongestAliphaticChain,
			CDKDescriptor.MDE, CDKDescriptor.MomentOfInertia, CDKDescriptor.PetitjeanNumber,
			CDKDescriptor.PetitjeanShapeIndex, CDKDescriptor.RotatableBondsCount, CDKDescriptor.RuleOfFive,
			CDKDescriptor.TPSA, CDKDescriptor.VAdjMa, CDKDescriptor.Weight, CDKDescriptor.WeightedPath,
			CDKDescriptor.WHIM, CDKDescriptor.WienerNumbers, CDKDescriptor.XLogP, CDKDescriptor.ZagrebIndex };

	public static int numFeatureValues(CDKDescriptor p)
	{
		switch (p)
		{
			case AminoAcidCount:
				return 20;
			case AutocorrelationDescriptorCharge:
				return 5;
			case AutocorrelationDescriptorMass:
				return 5;
			case AutocorrelationDescriptorPolarizability:
				return 5;
			case BCUT:
				return 6;
			case CarbonTypes:
				return 9;
			case ChiChain:
				return 10;
			case ChiCluster:
				return 8;
			case ChiPathCluster:
				return 6;
			case ChiPath:
				return 16;
			case CPSA:
				return 29;
			case GravitationalIndex:
				return 9;
			case KappaShapeIndices:
				return 3;
			case KierHallSmarts:
				return 79;
			case LengthOverBreadth:
				return 2;
			case MDE:
				return 19;
			case MomentOfInertia:
				return 7;
			case PetitjeanShapeIndex:
				return 2;
			case WeightedPath:
				return 5;
			case WHIM:
				return 17;
			case WienerNumbers:
				return 2;
		}
		return 1;
	}

	CDKDescriptor desc;
	int index;
	public static CDKProperty SMILES = new CDKProperty(CDKDescriptor.SMILES, 0);

	public CDKProperty(CDKDescriptor desc, int index)
	{
		this.desc = desc;
		this.index = index;
	}

	public String toString()
	{
		return desc + "_" + index;
	}

	public static CDKProperty fromString(String s)
	{
		String split[] = s.split("_");
		return new CDKProperty(CDKDescriptor.valueOf(split[0]), Integer.parseInt(split[1]));
	}

	@Override
	public boolean equals(Object o)
	{
		return (o instanceof CDKProperty) && ((CDKProperty) o).desc.equals(desc) && ((CDKProperty) o).index == index;
	}

	@Override
	public boolean isNumeric()
	{
		return true;
	}

	public IMolecularDescriptor newMolecularDescriptor()
	{
		try
		{
			Class<?> c = null;
			try
			{
				c = Class.forName("org.openscience.cdk.qsar.descriptors.molecular." + desc + "Descriptor");
			}
			catch (ClassNotFoundException e)
			{
				c = Class.forName("org.openscience.cdk.qsar.descriptors.molecular." + desc);
			}
			return (IMolecularDescriptor) c.newInstance();
		}
		catch (Exception e2)
		{
			e2.printStackTrace();
			return null;
		}
	}

}
