package data;

import java.util.ArrayList;
import java.util.List;

import org.openscience.cdk.qsar.IMolecularDescriptor;

import util.ArrayUtil;
import dataInterface.AbstractMoleculeProperty;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculePropertySet;

public class CDKProperty extends AbstractMoleculeProperty
{

	CDKDescriptorClass desc;
	int index;
	private static List<CDKProperty> instances = new ArrayList<CDKProperty>();

	private static enum CDKDescriptorClass
	{
		SMILES, ALOGP, AminoAcidCount, APol, AromaticAtomsCount, AromaticBondsCount, AtomCount,
		AutocorrelationDescriptorCharge, AutocorrelationDescriptorMass, AutocorrelationDescriptorPolarizability, BCUT,
		BondCount, BPol, CarbonTypes, ChiChain, ChiCluster, ChiPathCluster, ChiPath, CPSA, EccentricConnectivityIndex,
		FragmentComplexity, GravitationalIndex, HBondAcceptorCount, HBondDonorCount, IPMolecularLearning,
		KappaShapeIndices, KierHallSmarts, LargestChain, LargestPiSystem, LengthOverBreadth, LongestAliphaticChain,
		MDE, MomentOfInertia, PetitjeanNumber, PetitjeanShapeIndex, RotatableBondsCount, RuleOfFive, TPSA, VAdjMa,
		Weight, WeightedPath, WHIM, WienerNumbers, XLogP, ZagrebIndex
	};

	private static CDKDescriptorClass[] CDK_NUMERIC_DESCRIPTORS = { CDKDescriptorClass.ALOGP, CDKDescriptorClass.APol,
			CDKDescriptorClass.AminoAcidCount, CDKDescriptorClass.AromaticAtomsCount,
			CDKDescriptorClass.AromaticBondsCount, CDKDescriptorClass.AtomCount,
			CDKDescriptorClass.AutocorrelationDescriptorCharge, CDKDescriptorClass.AutocorrelationDescriptorMass,
			CDKDescriptorClass.AutocorrelationDescriptorPolarizability, CDKDescriptorClass.BCUT,
			CDKDescriptorClass.BondCount, CDKDescriptorClass.BPol, CDKDescriptorClass.CarbonTypes,
			CDKDescriptorClass.ChiChain,
			CDKDescriptorClass.ChiCluster,
			CDKDescriptorClass.ChiPathCluster,
			CDKDescriptorClass.ChiPath,
			CDKDescriptorClass.CPSA,
			CDKDescriptorClass.EccentricConnectivityIndex,
			CDKDescriptorClass.FragmentComplexity,
			CDKDescriptorClass.GravitationalIndex,
			CDKDescriptorClass.HBondAcceptorCount,
			CDKDescriptorClass.HBondDonorCount,
			//			CDKDescriptorClass.IPMolecularLearning,
			CDKDescriptorClass.KappaShapeIndices, CDKDescriptorClass.KierHallSmarts, CDKDescriptorClass.LargestChain,
			CDKDescriptorClass.LargestPiSystem, CDKDescriptorClass.LengthOverBreadth,
			CDKDescriptorClass.LongestAliphaticChain, CDKDescriptorClass.MDE, CDKDescriptorClass.MomentOfInertia,
			CDKDescriptorClass.PetitjeanNumber, CDKDescriptorClass.PetitjeanShapeIndex,
			CDKDescriptorClass.RotatableBondsCount, CDKDescriptorClass.RuleOfFive, CDKDescriptorClass.TPSA,
			CDKDescriptorClass.VAdjMa, CDKDescriptorClass.Weight, CDKDescriptorClass.WeightedPath,
			CDKDescriptorClass.WHIM, CDKDescriptorClass.WienerNumbers, CDKDescriptorClass.XLogP,
			CDKDescriptorClass.ZagrebIndex };

	public static CDKProperty SMILES = CDKProperty.create(CDKDescriptorClass.SMILES, 0);

	private CDKProperty(CDKDescriptorClass desc, int index)
	{
		this.desc = desc;
		this.index = index;

		if (ArrayUtil.indexOf(CDK_NUMERIC_DESCRIPTORS, desc) != -1)
		{
			setTypeAllowed(Type.NOMINAL, false);
			setType(Type.NUMERIC);
		}
		else
		{
			setTypeAllowed(Type.NUMERIC, false);
			setType(Type.NOMINAL);
		}
	}

	public static CDKProperty create(CDKDescriptorClass desc, int index)
	{
		CDKProperty p = new CDKProperty(desc, index);
		if (instances.indexOf(p) == -1)
		{
			instances.add(p);
			return p;
		}
		else
		{
			return instances.get(instances.indexOf(p));
		}
	}

	public String toString()
	{
		return desc + "_" + index;
	}

	public int numSetValues()
	{
		return new CDKDescriptor(desc).getSize();
	}

	public static CDKProperty fromString(String s)
	{
		String split[] = s.split("_");
		return new CDKProperty(CDKDescriptorClass.valueOf(split[0]), Integer.parseInt(split[1]));
	}

	@Override
	public boolean equals(Object o)
	{
		return (o instanceof CDKProperty) && ((CDKProperty) o).desc.equals(desc) && ((CDKProperty) o).index == index;
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

	public static final CDKDescriptor[] DESCRIPTORS = new CDKDescriptor[CDKDescriptorClass.values().length];
	public static final CDKDescriptor[] NUMERIC_DESCRIPTORS = new CDKDescriptor[CDK_NUMERIC_DESCRIPTORS.length];
	static
	{
		int count = 0;
		for (CDKDescriptorClass d : CDKDescriptorClass.values())
			DESCRIPTORS[count++] = new CDKDescriptor(d);
		count = 0;
		for (CDKDescriptorClass d : CDK_NUMERIC_DESCRIPTORS)
			NUMERIC_DESCRIPTORS[count++] = new CDKDescriptor(d);
	}

	public static class CDKDescriptor implements MoleculePropertySet
	{
		CDKDescriptorClass desc;

		public CDKDescriptor(CDKDescriptorClass desc)
		{
			this.desc = desc;
		}

		@Override
		public int getSize()
		{
			switch (desc)
			{
				case ALOGP:
					return 3;
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

		@Override
		public MoleculeProperty get(int index)
		{
			return CDKProperty.create(desc, index);
		}

		public String toString()
		{
			return desc.toString();
		}

		public static CDKDescriptor fromString(String s)
		{
			return new CDKDescriptor(CDKDescriptorClass.valueOf(s));
		}

		@Override
		public boolean equals(Object o)
		{
			return (o instanceof CDKDescriptor) && ((CDKDescriptor) o).desc.equals(desc);
		}
	}
}
