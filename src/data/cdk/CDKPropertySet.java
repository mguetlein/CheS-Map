package data.cdk;

import gui.binloc.Binary;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.Settings;
import main.TaskProvider;

import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.qsar.IMolecularDescriptor;

import util.DoubleKeyHashMap;
import util.FileUtil.UnexpectedNumColsException;
import util.ValueFileCache;
import data.DatasetFile;
import data.desc.DescriptorForMixturesHandler;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.SubstructureType;
import dataInterface.CompoundProperty.Type;
import dataInterface.CompoundPropertySet;

public class CDKPropertySet implements CompoundPropertySet
{
	public static final CDKPropertySet[] DESCRIPTORS = new CDKPropertySet[CDKDescriptor.getDescriptors().length];
	public static final CDKPropertySet[] NUMERIC_DESCRIPTORS = new CDKPropertySet[CDKDescriptor.getNumericDescriptors().length];
	static
	{
		int count = 0;
		for (CDKDescriptor d : CDKDescriptor.getDescriptors())
			DESCRIPTORS[count++] = new CDKPropertySet(d);
		count = 0;
		for (CDKDescriptor d : CDKDescriptor.getNumericDescriptors())
			NUMERIC_DESCRIPTORS[count++] = new CDKPropertySet(d);
	}

	private CDKDescriptor desc;

	public CDKPropertySet(CDKDescriptor desc)
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
		return desc.getSize();
	}

	private static DoubleKeyHashMap<String, DatasetFile, CDKProperty> props = new DoubleKeyHashMap<String, DatasetFile, CDKProperty>();

	public static CDKProperty createCDKProperty(CDKDescriptor desc, DatasetFile dataset, int index)
	{
		if (!props.containsKeyPair(desc.getFeatureName(index), dataset))
			props.put(desc.getFeatureName(index), dataset, new CDKProperty(desc, index));
		return props.get(desc.getFeatureName(index), dataset);
	}

	@Override
	public CompoundProperty get(DatasetFile dataset, int index)
	{
		return createCDKProperty(desc, dataset, index);
	}

	public String toString()
	{
		return desc.toString();
	}

	public static CDKPropertySet fromString(String s)
	{
		CDKDescriptor desc = CDKDescriptor.fromString(s);
		if (desc == null)
			return null;
		return new CDKPropertySet(desc);
	}

	public static CDKProperty fromString(String s, Type t, DatasetFile dataset)
	{
		CDKProperty p = CDKPropertySet.fromFeatureName(s, dataset);
		if (!p.isTypeAllowed(t))
			throw new IllegalArgumentException();
		p.setType(t);
		return p;
	}

	public static CDKProperty fromFeatureName(String s, DatasetFile dataset)
	{
		for (CDKDescriptor d : CDKDescriptor.getNumericDescriptors())
			for (int i = 0; i < d.getSize(); i++)
				if (d.getFeatureName(i).equals(s))
					return createCDKProperty(d, dataset, i);
		return null;
	}

	@Override
	public boolean equals(Object o)
	{
		return (o instanceof CDKPropertySet) && ((CDKPropertySet) o).desc.equals(desc);
	}

	@Override
	public String getDescription()
	{
		return desc.getDescription();
	}

	public String[] getDictionaryClass()
	{
		return desc.getDictionaryClass();
	}

	@Override
	public Type getType()
	{
		if (desc.isNumeric())
			return Type.NUMERIC;
		else
			return Type.NOMINAL;
	}

	@Override
	public boolean isSizeDynamic()
	{
		return false;
	}

	@Override
	public boolean isComputed(DatasetFile dataset)
	{
		return createCDKProperty(desc, dataset, 0).isValuesSet();
	}

	private String cacheFile(DatasetFile dataset)
	{
		return dataset.getFeatureValuesFilePath(this);
	}

	@Override
	public boolean isCached(DatasetFile dataset)
	{
		return new File(cacheFile(dataset)).exists();
	}

	@Override
	public boolean compute(DatasetFile dataset)
	{
		if (isComputed(dataset))
			throw new IllegalStateException();

		IMolecule mols[] = dataset.getCompounds();

		String cache = cacheFile(dataset);

		List<Double[]> vv = null;
		if (Settings.CACHING_ENABLED && new File(cache).exists())
		{
			Settings.LOGGER.info("reading cdk props from: " + cache);
			try
			{
				vv = ValueFileCache.readCacheDouble(cache, mols.length);
			}
			catch (UnexpectedNumColsException e)
			{
				Settings.LOGGER.error(e);
			}
		}
		if (vv == null)
		{
			IMolecularDescriptor descriptor = desc.getIMolecularDescriptor();
			if (descriptor == null)
				throw new IllegalStateException("Not a CDK molecular descriptor: " + this);

			vv = new ArrayList<Double[]>();
			for (int j = 0; j < getSize(); j++)
				vv.add(new Double[mols.length]);

			TaskProvider.debug("Compute " + this + " for " + mols.length + " compounds");

			for (int i = 0; i < mols.length; i++)
			{
				TaskProvider.verbose("Compute " + this + " for " + (i + 1) + "/" + mols.length + " compounds");

				if (mols[i].getAtomCount() == 0)
				{
					for (int j = 0; j < getSize(); j++)
						vv.get(j)[i] = null;
				}
				else
				{
					try
					{
						Double d[];
						if (Settings.DESC_MIXTURE_HANDLING)
							d = DescriptorForMixturesHandler.computeCDKDescriptor(desc, mols[i]);
						else
							d = desc.computeDescriptor(mols[i]);
						if (getSize() != d.length)
							throw new IllegalStateException("num feature values wrong for '" + this + "' : "
									+ getSize() + " != " + d.length);
						for (int j = 0; j < d.length; j++)
							vv.get(j)[i] = d[j];
					}
					catch (Throwable e)
					{
						TaskProvider.warning("Could not compute cdk feature " + this, e);
						for (int j = 0; j < getSize(); j++)
							vv.get(j)[i] = null;
					}
				}

				for (int j = 0; j < getSize(); j++)
					if (vv.get(j)[i] != null && (vv.get(j)[i].isNaN() || vv.get(j)[i].isInfinite()))
						vv.get(j)[i] = null;

				if (!TaskProvider.isRunning())
					return false;
			}

			for (int j = 0; j < getSize(); j++)
				checkForBugs(j, vv.get(j));

			Settings.LOGGER.info("writing cdk props to: " + cache);
			ValueFileCache.writeCacheDouble(cache, vv);
		}
		for (int j = 0; j < getSize(); j++)
			createCDKProperty(desc, dataset, j).setDoubleValues(vv.get(j));

		return true;
	}

	private void checkForBugs(int index, Double values[])
	{
		HashMap<String, Double[]> filter = new HashMap<String, Double[]>();
		filter.put("ATSc1", new Double[] { null, 100.0 });
		filter.put("ATSc2", new Double[] { -100.0, null });
		filter.put("ATSc3", new Double[] { null, 10.0 });
		filter.put("BCUTc-1l", new Double[] { -0.5, null });
		filter.put("BCUTc-1h", new Double[] { null, 1.0 });
		filter.put("ECCEN", new Double[] { 0.0, 100000.0 });
		filter.put("topoShape", new Double[] { null, 1.0 });
		filter.put("Weta1.unity", new Double[] { null, 2.0 });
		filter.put("Weta2.unity", new Double[] { null, 1.0 });
		filter.put("WD.unity", new Double[] { null, 3.0 });
		filter.put("WPATH", new Double[] { null, 100000.0 });
		for (String f : filter.keySet())
		{
			if (desc.getFeatureName(index).equals(f))
			{
				System.out.println("checking " + f);
				Double min = filter.get(f)[0];
				Double max = filter.get(f)[1];
				for (int i = 0; i < values.length; i++)
				{
					if (values[i] != null)
					{
						if (min != null && values[i] < min)
						{
							System.out.println("filtering " + f + " " + values[i] + " should not be < " + min);
							values[i] = null;
						}
						else if (max != null && values[i] > max)
						{
							System.out.println("filtering " + f + " " + values[i] + " should not be > " + max);
							values[i] = null;
						}
					}
				}
			}
		}
	}

	@Override
	public boolean isSelectedForMapping()
	{
		return true;
	}

	@Override
	public Binary getBinary()
	{
		return null;
	}

	@Override
	public SubstructureType getSubstructureType()
	{
		return null;
	}

	@Override
	public String getNameIncludingParams()
	{
		String s = toString();
		if (Settings.DESC_MIXTURE_HANDLING)
			s += ".mixt";
		return s;
	}

	@Override
	public boolean isSizeDynamicHigh(DatasetFile dataset)
	{
		return false;
	}

	@Override
	public boolean isComputationSlow()
	{
		return desc.isComputationSlow();
	}

	@Override
	public boolean isSensitiveTo3D()
	{
		return true;
	}

}