package data;

import gui.binloc.Binary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.TaskProvider;

import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.qsar.DescriptorSpecification;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.DoubleArrayResult;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.IDescriptorResult;
import org.openscience.cdk.qsar.result.IntegerArrayResult;
import org.openscience.cdk.qsar.result.IntegerResult;
import org.openscience.cdk.smiles.SmilesGenerator;

import util.ArrayUtil;
import dataInterface.AbstractMoleculeProperty;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculePropertySet;

public class CDKProperty extends AbstractMoleculeProperty
{

	private CDKDescriptorClass desc;
	private int index;

	private static enum CDKDescriptorClass
	{
		SMILES, ALOGP, AminoAcidCount, APol, AromaticAtomsCount, AromaticBondsCount, AtomCount,
		AutocorrelationDescriptorCharge, AutocorrelationDescriptorMass, AutocorrelationDescriptorPolarizability, BCUT,
		BondCount, BPol, CarbonTypes, ChiChain, ChiCluster, ChiPathCluster, ChiPath, CPSA, EccentricConnectivityIndex,
		FragmentComplexity, GravitationalIndex, HBondAcceptorCount, HBondDonorCount, IPMolecularLearning,
		KappaShapeIndices, KierHallSmarts, LargestChain, LargestPiSystem, LengthOverBreadth, LongestAliphaticChain,
		MDE, MomentOfInertia, PetitjeanNumber, PetitjeanShapeIndex, RotatableBondsCount, RuleOfFive, TPSA, VABC,
		VAdjMa, Weight, WeightedPath, WHIM, WienerNumbers, XLogP, ZagrebIndex
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
			CDKDescriptorClass.VABC, CDKDescriptorClass.VAdjMa, CDKDescriptorClass.Weight,
			CDKDescriptorClass.WeightedPath, CDKDescriptorClass.WHIM, CDKDescriptorClass.WienerNumbers,
			CDKDescriptorClass.XLogP, CDKDescriptorClass.ZagrebIndex };

	private static HashMap<String, CDKProperty> instances = new HashMap<String, CDKProperty>();
	public static CDKProperty SMILES = CDKProperty.create(CDKDescriptorClass.SMILES, 0);

	private CDKProperty(CDKDescriptorClass desc, int index)
	{
		super(desc + "_" + index, "CDK Descriptor");

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
		if (!instances.containsKey(desc + "_" + index))
			instances.put(desc + "_" + index, new CDKProperty(desc, index));
		return instances.get(desc + "_" + index);
	}

	@Override
	public CDKDescriptor getMoleculePropertySet()
	{
		return new CDKDescriptor(desc);
	}

	public static CDKProperty fromString(String s, Type t)
	{
		String split[] = s.split("_");
		CDKProperty p = new CDKProperty(CDKDescriptorClass.valueOf(split[0]), Integer.parseInt(split[1]));
		if (!p.isTypeAllowed(t))
			throw new IllegalArgumentException();
		p.setType(t);
		return p;
	}

	@Override
	public boolean equals(Object o)
	{
		return (o instanceof CDKProperty) && ((CDKProperty) o).desc.equals(desc) && ((CDKProperty) o).index == index;
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
		private CDKDescriptorClass desc;

		public CDKDescriptor(CDKDescriptorClass desc)
		{
			this.desc = desc;
		}

		@Override
		public int getSize(DatasetFile dataset)
		{
			return getSize();
		}

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
		public MoleculeProperty get(DatasetFile dataset, int index)
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

		@Override
		public String getDescription()
		{
			IMolecularDescriptor descriptor = newMolecularDescriptor(desc);
			DescriptorSpecification spec = descriptor.getSpecification();
			String s = toString() + "\n";
			s += "Reference: " + spec.getSpecificationReference() + "\n";
			s += "Implementation Title: " + spec.getImplementationTitle() + "\n";
			//			s += "Identifier: " + spec.getImplementationIdentifier() + "\n";
			//			s += "Vendor: " + spec.getImplementationVendor() + "\n";
			return s;
		}

		@Override
		public Type getType()
		{
			if (ArrayUtil.indexOf(CDK_NUMERIC_DESCRIPTORS, desc) != -1)
				return Type.NUMERIC;
			else
				return Type.NOMINAL;
		}

		public IMolecularDescriptor newMolecularDescriptor()
		{
			return newMolecularDescriptor(desc);
		}

		private static IMolecularDescriptor newMolecularDescriptor(CDKDescriptorClass desc)
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

		@Override
		public boolean isSizeDynamic()
		{
			return false;
		}

		@Override
		public boolean isComputed(DatasetFile dataset)
		{
			return CDKProperty.create(desc, 0).isValuesSet(dataset);
		}

		@Override
		public boolean compute(DatasetFile dataset)
		{
			if (isComputed(dataset))
				throw new IllegalStateException();

			IMolecule mols[] = dataset.getMolecules();

			if (desc == CDKDescriptorClass.SMILES)
			{
				SmilesGenerator sg = new SmilesGenerator();
				String smiles[] = new String[mols.length];
				for (int i = 0; i < mols.length; i++)
					smiles[i] = sg.createSMILES(mols[i]);
				CDKProperty.SMILES.setStringValues(dataset, smiles);
			}
			else
			{
				IMolecularDescriptor descriptor = newMolecularDescriptor();
				if (descriptor == null)
					throw new IllegalStateException("Not a CDK molecular descriptor: " + this);

				List<Double[]> vv = new ArrayList<Double[]>();
				for (int j = 0; j < getSize(); j++)
					vv.add(new Double[mols.length]);

				for (int i = 0; i < mols.length; i++)
				{
					TaskProvider.task().verbose(
							"Compute " + this + " for " + (i + 1) + "/" + mols.length + " compounds");

					if (mols[i].getAtomCount() == 0)
					{
						for (int j = 0; j < getSize(); j++)
							vv.get(j)[i] = null;
					}
					else
					{
						try
						{
							IDescriptorResult res = descriptor.calculate(mols[i]).getValue();
							if (res instanceof IntegerResult)
								vv.get(0)[i] = (double) ((IntegerResult) res).intValue();
							else if (res instanceof DoubleResult)
								vv.get(0)[i] = ((DoubleResult) res).doubleValue();
							else if (res instanceof DoubleArrayResult)
							{
								if (getSize() != ((DoubleArrayResult) res).length())
									throw new IllegalStateException("num feature values wrong for '" + this + "' : "
											+ getSize() + " != " + ((DoubleArrayResult) res).length());
								for (int j = 0; j < getSize(); j++)
									vv.get(j)[i] = ((DoubleArrayResult) res).get(j);
							}
							else if (res instanceof IntegerArrayResult)
							{
								if (getSize() != ((IntegerArrayResult) res).length())
									throw new IllegalStateException("num feature values wrong for '" + this + "' : "
											+ getSize() + " != " + ((IntegerArrayResult) res).length());
								for (int j = 0; j < getSize(); j++)
									vv.get(j)[i] = (double) ((IntegerArrayResult) res).get(j);
							}
							else
								throw new IllegalStateException("Unknown idescriptor result value for '" + this
										+ "' : " + res.getClass());

						}
						catch (Throwable e)
						{
							TaskProvider.task().warning("Could not compute cdk feature " + this, e);
							for (int j = 0; j < getSize(); j++)
								vv.get(j)[i] = null;
						}
					}

					for (int j = 0; j < getSize(); j++)
						if (vv.get(j)[i] != null && (vv.get(j)[i].isNaN() || vv.get(j)[i].isInfinite()))
							vv.get(j)[i] = null;

					if (TaskProvider.task().isCancelled())
						return false;
				}
				for (int j = 0; j < getSize(); j++)
					CDKProperty.create(desc, j).setDoubleValues(dataset, vv.get(j));
			}
			return true;
		}

		@Override
		public boolean isUsedForMapping()
		{
			return true;
		}

		@Override
		public Binary getBinary()
		{
			return null;
		}
	}
}
