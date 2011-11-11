package data.cdk;

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
		List<IDescriptor> l = ENGINE.getDescriptorInstances();
		CDK_DESCRIPTORS = new CDKDescriptor[l.size()];
		int i = 0;
		for (IDescriptor c : l)
			CDK_DESCRIPTORS[i++] = new CDKDescriptor((IMolecularDescriptor) c);

		List<CDKDescriptor> numDesc = new ArrayList<CDKDescriptor>();
		for (CDKDescriptor desc : CDK_DESCRIPTORS)
			if (desc.numeric)
				numDesc.add(desc);
		CDK_NUMERIC_DESCRIPTORS = new CDKDescriptor[numDesc.size()];
		numDesc.toArray(CDK_NUMERIC_DESCRIPTORS);
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
}