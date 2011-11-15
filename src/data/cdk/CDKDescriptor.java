package data.cdk;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.openscience.cdk.qsar.DescriptorEngine;
import org.openscience.cdk.qsar.IDescriptor;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.ChiChainDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.ChiClusterDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.ChiPathClusterDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.ChiPathDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.FMFDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.IPMolecularLearningDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.WeightedPathDescriptor;
import org.openscience.cdk.qsar.result.BooleanResultType;
import org.openscience.cdk.qsar.result.DoubleArrayResultType;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.DoubleResultType;
import org.openscience.cdk.qsar.result.IDescriptorResult;
import org.openscience.cdk.qsar.result.IntegerArrayResultType;
import org.openscience.cdk.qsar.result.IntegerResult;
import org.openscience.cdk.qsar.result.IntegerResultType;

import util.ArrayUtil;

class CDKDescriptor
{
	public static final DescriptorEngine ENGINE = new DescriptorEngine(DescriptorEngine.MOLECULAR);
	static CDKDescriptor[] CDK_DESCRIPTORS;
	static CDKDescriptor[] CDK_NUMERIC_DESCRIPTORS;

	static
	{
		try
		{
			List<IDescriptor> descriptorList = new ArrayList<IDescriptor>();
			BufferedReader buffy = new BufferedReader(new InputStreamReader(
					IMolecularDescriptor.class.getResourceAsStream("/qsar-descriptors.set")));
			String s = null;
			while ((s = buffy.readLine()) != null)
				if (s.contains("descriptors.molecular"))
					try
					{
						descriptorList.add((IMolecularDescriptor) Class.forName(s).newInstance());
					}
					catch (Exception e)
					{
						//						System.err.println("could not init descriptor: " + s);
						//						e.printStackTrace();
					}
			System.out.println("loaded " + descriptorList.size() + " cdk descriptors");

			//			List<IDescriptor> l = ENGINE.getDescriptorInstances(); // not working in webstart

			// hard coded, if the above solution does not work
			//			IMolecularDescriptor l[] = new IMolecularDescriptor[] { new ALOGPDescriptor(), new APolDescriptor(),
			//					new AcidicGroupCountDescriptor(), new AromaticAtomsCountDescriptor(),
			//					new AromaticBondsCountDescriptor(), new AtomCountDescriptor(),
			//					new AutocorrelationDescriptorCharge(), new AutocorrelationDescriptorMass(),
			//					new AutocorrelationDescriptorPolarizability(), new BCUTDescriptor(), new BPolDescriptor(),
			//					new BasicGroupCountDescriptor(), new BondCountDescriptor(), new CPSADescriptor(),
			//					new CarbonTypesDescriptor(), new ChiChainDescriptor(), new ChiClusterDescriptor(),
			//					new ChiPathClusterDescriptor(), new ChiPathDescriptor(),
			//					new EccentricConnectivityIndexDescriptor(), new FMFDescriptor(),
			//					new FragmentComplexityDescriptor(), new GravitationalIndexDescriptor(),
			//					new HBondAcceptorCountDescriptor(), new HBondDonorCountDescriptor(),
			//					new HybridizationRatioDescriptor(), new IPMolecularLearningDescriptor(),
			//					new KappaShapeIndicesDescriptor(), new KierHallSmartsDescriptor(), new LargestChainDescriptor(),
			//					new LargestPiSystemDescriptor(), new LengthOverBreadthDescriptor(),
			//					new LongestAliphaticChainDescriptor(), new MDEDescriptor(), new MannholdLogPDescriptor(),
			//					new MomentOfInertiaDescriptor(), new PetitjeanNumberDescriptor(),
			//					new PetitjeanShapeIndexDescriptor(), new RotatableBondsCountDescriptor(),
			//					new RuleOfFiveDescriptor(), new TPSADescriptor(), new VABCDescriptor(), new VAdjMaDescriptor(),
			//					new WHIMDescriptor(), new WeightDescriptor(), new WeightedPathDescriptor(),
			//					new WienerNumbersDescriptor(), new XLogPDescriptor(), new ZagrebIndexDescriptor() };

			CDK_DESCRIPTORS = new CDKDescriptor[descriptorList.size()];
			int i = 0;
			for (IDescriptor c : descriptorList)
			{
				CDK_DESCRIPTORS[i++] = new CDKDescriptor((IMolecularDescriptor) c);
				//				System.out.print("new " + c.getClass().getSimpleName() + "(), ");
			}

			List<CDKDescriptor> numDesc = new ArrayList<CDKDescriptor>();
			for (CDKDescriptor desc : CDK_DESCRIPTORS)
				if (desc.numeric)
					numDesc.add(desc);
			CDK_NUMERIC_DESCRIPTORS = new CDKDescriptor[numDesc.size()];
			numDesc.toArray(CDK_NUMERIC_DESCRIPTORS);
		}
		catch (Exception e)
		{
			System.err.println("could not load CDK descriptors");
			e.printStackTrace();
			CDK_DESCRIPTORS = new CDKDescriptor[0];
			CDK_NUMERIC_DESCRIPTORS = new CDKDescriptor[0];
		}
	}

	private IMolecularDescriptor m;
	private int size;
	private boolean numeric;
	private String name;
	private String description;
	private String dictionaryClass[];
	private boolean slow;

	public CDKDescriptor(IMolecularDescriptor m)
	{
		this.m = m;
		name = CDKDescriptor.ENGINE.getDictionaryTitle(m.getSpecification()).trim();
		//m.getClass().getSimpleName().replace("Descriptor", "");

		description = name + " ";
		dictionaryClass = CDKDescriptor.ENGINE.getDictionaryClass(m.getSpecification());
		for (int i = 0; i < dictionaryClass.length; i++)
			dictionaryClass[i] = dictionaryClass[i].replace("Descriptor", "");
		description += ArrayUtil.toString(dictionaryClass, ",", "(", ")") + "\n";
		description += CDKDescriptor.ENGINE.getDictionaryDefinition(m.getSpecification()).trim();
		description += "API: http://pele.farmbio.uu.se/nightly/api/" + m.getClass().getName().replaceAll("\\.", "/")
				+ "\n";

		slow = m instanceof IPMolecularLearningDescriptor || m instanceof ChiChainDescriptor
				|| m instanceof ChiClusterDescriptor || m instanceof ChiPathDescriptor
				|| m instanceof ChiPathClusterDescriptor || m instanceof FMFDescriptor
				|| m instanceof WeightedPathDescriptor;

		IDescriptorResult r = m.getDescriptorResultType();
		if (r instanceof BooleanResultType)
		{
			size = 1;
			numeric = false;
		}
		else if (r instanceof DoubleArrayResultType)
		{
			size = ((DoubleArrayResultType) r).length();
			numeric = true;
		}
		else if (r instanceof IntegerArrayResultType)
		{
			size = ((IntegerArrayResultType) r).length();
			numeric = true;
		}
		else if (r instanceof DoubleResultType)
		{
			size = 1;
			numeric = true;
		}
		else if (r instanceof IntegerResultType)
		{
			size = 1;
			numeric = true;
		}
		else if (r instanceof IntegerResult)
		{
			size = 1;
			numeric = true;
		}
		else if (r instanceof DoubleResult)
		{
			size = 1;
			numeric = true;
		}
	}

	public static CDKDescriptor fromString(String s)
	{
		for (CDKDescriptor desc : CDK_DESCRIPTORS)
			if (desc.toString().equals(s))
				return desc;
		return null;
	}

	public int indexFromFeatureName(String featureName)
	{
		for (int i = 0; i < size; i++)
			if (getFeatureName(i).equals(featureName))
				return i;
		return -1;
	}

	public String toString()
	{
		return name;
	}

	public int getSize()
	{
		return size;
	}

	public IMolecularDescriptor getIMolecularDescriptor()
	{
		return m;
	}

	public boolean isNumeric()
	{
		return numeric;
	}

	public String getFeatureName(int index)
	{
		return m.getDescriptorNames()[index];
	}

	public String getDescription()
	{
		return description;
	}

	public String[] getDictionaryClass()
	{
		return dictionaryClass;
	}

	public boolean isComputationSlow()
	{
		return slow;
	}

	public static void main(String args[])
	{

	}
}