package data.cdk;

import gui.binloc.Binary;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import main.Settings;
import main.TaskProvider;

import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.DoubleArrayResult;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.IDescriptorResult;
import org.openscience.cdk.qsar.result.IntegerArrayResult;
import org.openscience.cdk.qsar.result.IntegerResult;

import util.FileUtil;
import util.StringUtil;
import util.ValueFileCache;
import data.DatasetFile;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import dataInterface.MoleculePropertySet;

public class CDKPropertySet implements MoleculePropertySet
{
	public static final CDKPropertySet[] DESCRIPTORS = new CDKPropertySet[CDKDescriptor.CDK_DESCRIPTORS.length];
	public static final CDKPropertySet[] NUMERIC_DESCRIPTORS = new CDKPropertySet[CDKDescriptor.CDK_NUMERIC_DESCRIPTORS.length];
	static
	{
		int count = 0;
		for (CDKDescriptor d : CDKDescriptor.CDK_DESCRIPTORS)
			DESCRIPTORS[count++] = new CDKPropertySet(d);
		count = 0;
		for (CDKDescriptor d : CDKDescriptor.CDK_NUMERIC_DESCRIPTORS)
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

	@Override
	public MoleculeProperty get(DatasetFile dataset, int index)
	{
		return CDKProperty.create(desc, index);
	}

	public String toString()
	{
		return desc.toString();
	}

	public static CDKPropertySet fromString(String s)
	{
		return new CDKPropertySet(CDKDescriptor.fromString(s));
	}

	public static CDKProperty fromFeatureName(String s)
	{
		for (CDKDescriptor d : CDKDescriptor.CDK_NUMERIC_DESCRIPTORS)
			for (int i = 0; i < d.getSize(); i++)
				if (d.getFeatureName(i).equals(s))
					return CDKProperty.create(d, i);
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
		return CDKProperty.create(desc, 0).isValuesSet(dataset);
	}

	private String cacheFile(DatasetFile dataset)
	{
		String sdfFile = dataset.getSDFPath(false);
		int index = sdfFile.lastIndexOf('.');
		if (index == -1)
			throw new IllegalStateException("filename has no '.'");
		return Settings.destinationFile(sdfFile, FileUtil.getFilename(sdfFile) + "." + dataset.getMD5() + "."
				+ StringUtil.encodeFilename(desc.toString()));
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

		IMolecule mols[] = dataset.getMolecules();

		String cache = cacheFile(dataset);

		List<Double[]> vv;
		if (new File(cache).exists())
		{
			System.out.println("reading cdk props from: " + cache);
			vv = ValueFileCache.readCacheDouble(cache);
		}
		else
		{
			IMolecularDescriptor descriptor = desc.getIMolecularDescriptor();
			if (descriptor == null)
				throw new IllegalStateException("Not a CDK molecular descriptor: " + this);

			vv = new ArrayList<Double[]>();
			for (int j = 0; j < getSize(); j++)
				vv.add(new Double[mols.length]);

			for (int i = 0; i < mols.length; i++)
			{
				TaskProvider.task().verbose("Compute " + this + " for " + (i + 1) + "/" + mols.length + " compounds");

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
							throw new IllegalStateException("Unknown idescriptor result value for '" + this + "' : "
									+ res.getClass());

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
			System.out.println("writing cdk props to: " + cache);
			ValueFileCache.writeCacheDouble(cache, vv);
		}
		for (int j = 0; j < getSize(); j++)
			CDKProperty.create(desc, j).setDoubleValues(dataset, vv.get(j));

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

	@Override
	public String getNameIncludingParams()
	{
		return toString();
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

}