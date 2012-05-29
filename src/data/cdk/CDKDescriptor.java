package data.cdk;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import main.Settings;

import org.openscience.cdk.qsar.DescriptorEngine;
import org.openscience.cdk.qsar.IDescriptor;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.IPMolecularLearningDescriptor;
import org.openscience.cdk.qsar.result.BooleanResultType;
import org.openscience.cdk.qsar.result.DoubleArrayResultType;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.DoubleResultType;
import org.openscience.cdk.qsar.result.IDescriptorResult;
import org.openscience.cdk.qsar.result.IntegerArrayResultType;
import org.openscience.cdk.qsar.result.IntegerResult;
import org.openscience.cdk.qsar.result.IntegerResultType;

import util.ArrayUtil;

public class CDKDescriptor
{
	public static final DescriptorEngine ENGINE = new DescriptorEngine(DescriptorEngine.MOLECULAR);
	static CDKDescriptor[] CDK_DESCRIPTORS;
	static CDKDescriptor[] CDK_NUMERIC_DESCRIPTORS;

	static
	{
		try
		{
			//			List<IDescriptor> descriptorList = ENGINE.getDescriptorInstances(); // not working in webstart
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
						//						Settings.LOGGER.warn("could not init descriptor: " + s);
						//						Settings.LOGGER.error(e);
					}
			buffy.close();
			Collections.sort(descriptorList, new Comparator<IDescriptor>()
			{
				@Override
				public int compare(IDescriptor o1, IDescriptor o2)
				{
					return o1.getClass().getName().compareTo(o2.getClass().getName());
				}
			});
			Settings.LOGGER.info("loaded " + descriptorList.size() + " cdk descriptors");

			CDK_DESCRIPTORS = new CDKDescriptor[descriptorList.size()];
			int i = 0;
			for (IDescriptor c : descriptorList)
			{
				CDK_DESCRIPTORS[i++] = new CDKDescriptor((IMolecularDescriptor) c);
				//				Settings.LOGGER.print("new " + c.getClass().getSimpleName() + "(), ");
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
			Settings.LOGGER.error("could not load CDK descriptors");
			Settings.LOGGER.error(e);
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

	public static String getAPILink(Class<?> clazz)
	{
		return "http://pele.farmbio.uu.se/nightly/api/" + clazz.getName().replaceAll("\\.", "/");
	}

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
		description += CDKDescriptor.ENGINE.getDictionaryDefinition(m.getSpecification()).trim() + "\n";
		description += "API: " + getAPILink(m.getClass()) + "\n";

		slow = m instanceof IPMolecularLearningDescriptor;
		//				|| m instanceof ChiChainDescriptor
		//				|| m instanceof ChiClusterDescriptor || m instanceof ChiPathDescriptor
		//				|| m instanceof ChiPathClusterDescriptor || m instanceof FMFDescriptor
		//				|| m instanceof WeightedPathDescriptor;

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