package data.obdesc;

import gui.binloc.Binary;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import main.BinHandler;
import main.Settings;
import main.TaskProvider;
import util.ArrayUtil;
import util.FileUtil.UnexpectedNumColsException;
import util.ListUtil;
import util.ValueFileCache;
import data.DatasetFile;
import data.FeatureService;
import dataInterface.AbstractMoleculeProperty;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculePropertySet;

public class OBDescriptorProperty extends AbstractMoleculeProperty implements MoleculePropertySet
{
	private static OBDescriptorProperty[] descriptors;

	public static OBDescriptorProperty[] getDescriptors(boolean forceReload)
	{
		if (descriptors == null || forceReload)
		{
			if (descriptors == null)
				descriptors = new OBDescriptorProperty[0];

			Set<String> descriptorIDs = OBDescriptorFactory.getDescriptorIDs(forceReload);
			if (descriptorIDs == null)
				return new OBDescriptorProperty[0];
			List<OBDescriptorProperty> desc = new ArrayList<OBDescriptorProperty>();
			for (String descriptorID : descriptorIDs)
			{
				OBDescriptorProperty p = fromString(descriptorID, null);
				if (p != null)
					desc.add(p);
				else
					desc.add(new OBDescriptorProperty(descriptorID, OBDescriptorFactory
							.getDescriptorDescription(descriptorID), OBDescriptorFactory
							.getDescriptorDefaultType(descriptorID)));
			}
			descriptors = ListUtil.toArray(desc);
		}
		return descriptors;
	}

	private String descriptorID;

	private OBDescriptorProperty(String descriptorID, String description, Type defaultType)
	{
		super(descriptorID, "OBDescriptor" + descriptorID, description + " (OB Descriptor)");
		this.descriptorID = descriptorID;

		if (defaultType == Type.NUMERIC)
		{
			setTypeAllowed(Type.NUMERIC, true);
			setTypeAllowed(Type.NOMINAL, false);
			setType(Type.NUMERIC);
		}
		else if (defaultType == Type.NOMINAL)
		{
			setTypeAllowed(Type.NUMERIC, false);
			setTypeAllowed(Type.NOMINAL, true);
			setType(Type.NOMINAL);
		}
		else
		{
			setTypeAllowed(Type.NUMERIC, true);
			setTypeAllowed(Type.NOMINAL, true);
		}
	}

	public String toString()
	{
		return getName();
	}

	public static OBDescriptorProperty fromString(String toString, Type type)
	{
		for (OBDescriptorProperty d : descriptors)
			if (d.getName().equals(toString))
			{
				if (type != null)
					d.setType(type);
				return d;
			}
		return null;
	}

	@Override
	public MoleculePropertySet getMoleculePropertySet()
	{
		return this;
	}

	@Override
	public boolean isComputed(DatasetFile dataset)
	{
		return isValuesSet(dataset);
	}

	@Override
	public boolean isCached(DatasetFile dataset)
	{
		return new File(cacheFile(dataset)).exists();
	}

	private String cacheFile(DatasetFile dataset)
	{
		return Settings.destinationFile(dataset, dataset.getShortName() + "." + dataset.getMD5() + "." + descriptorID);
	}

	@Override
	public boolean compute(DatasetFile dataset)
	{
		String cache = cacheFile(dataset);
		String vals[] = null;
		if (Settings.CACHING_ENABLED && new File(cache).exists())
		{
			Settings.LOGGER.info("reading ob descriptors from: " + cache);
			try
			{
				vals = ValueFileCache.readCacheString(cache, dataset.numCompounds()).get(0);
			}
			catch (UnexpectedNumColsException e)
			{
				Settings.LOGGER.error(e);
			}
		}
		if (vals == null)
		{
			vals = OBDescriptorFactory.compute(dataset.getSDFPath(false), descriptorID);
			Settings.LOGGER.info("writing ob descriptors to: " + cache);
			ValueFileCache.writeCacheString(cache, vals);
		}

		int numDistinct = -1;
		if (isTypeAllowed(Type.NOMINAL))
		{
			setStringValues(dataset, vals);
		}
		if (isTypeAllowed(Type.NUMERIC))
		{
			Double dVals[] = ArrayUtil.parse(vals);
			if (dVals == null)
			{
				if (getType() == Type.NUMERIC)
				{
					TaskProvider.warning(
							"Cannot compute feature: " + getName(),
							"Numeric features cannot be parsed. Values returned from OpenBabel:\n"
									+ ArrayUtil.toString(vals));
					new File(cache).delete();
					return false;
				}
				else
					setTypeAllowed(Type.NUMERIC, false);
			}
			else
				setDoubleValues(dataset, dVals);
		}
		if (getType() == null)
		{
			if (isTypeAllowed(Type.NOMINAL)
					&& FeatureService.guessNominalFeatureType(numDistinct, vals.length, isTypeAllowed(Type.NUMERIC)))
				setType(Type.NOMINAL);
			else if (isTypeAllowed(Type.NUMERIC))
				setType(Type.NUMERIC);
		}
		return true;
	}

	@Override
	public boolean isSizeDynamic()
	{
		return false;
	}

	@Override
	public boolean isSizeDynamicHigh(DatasetFile dataset)
	{
		return false;
	}

	@Override
	public int getSize(DatasetFile d)
	{
		return 1;
	}

	@Override
	public MoleculeProperty get(DatasetFile d, int index)
	{
		if (index != 0)
			throw new IllegalStateException();
		return this;
	}

	@Override
	public Binary getBinary()
	{
		return BinHandler.BABEL_BINARY;
	}

	@Override
	public boolean isUsedForMapping()
	{
		return true;
	}

	@Override
	public String getNameIncludingParams()
	{
		return getUniqueName() + "_" + getType();
	}

	@Override
	public boolean isComputationSlow()
	{
		return false;
	}

}
