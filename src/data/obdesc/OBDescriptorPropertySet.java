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
import util.DoubleKeyHashMap;
import util.FileUtil.UnexpectedNumColsException;
import util.ListUtil;
import util.ValueFileCache;
import data.DatasetFile;
import data.FeatureService;
import data.desc.DescriptorForMixturesHandler;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.SubstructureType;
import dataInterface.CompoundProperty.Type;
import dataInterface.CompoundPropertySet;

public class OBDescriptorPropertySet implements CompoundPropertySet
{
	private static OBDescriptorPropertySet[] descriptors;

	public synchronized static OBDescriptorPropertySet[] getDescriptors(boolean forceReload)
	{
		if (descriptors == null || forceReload)
		{
			if (descriptors == null)
				descriptors = new OBDescriptorPropertySet[0];

			Set<String> descriptorIDs = OBDescriptorFactory.getDescriptorIDs(forceReload);
			if (descriptorIDs == null)
				return new OBDescriptorPropertySet[0];
			List<OBDescriptorPropertySet> desc = new ArrayList<OBDescriptorPropertySet>();
			for (String descriptorID : descriptorIDs)
			{
				OBDescriptorPropertySet p = fromString(descriptorID, null);
				if (p != null)
					desc.add(p);
				else
					desc.add(new OBDescriptorPropertySet(descriptorID, OBDescriptorFactory
							.getDescriptorDescription(descriptorID), OBDescriptorFactory
							.getDescriptorDefaultType(descriptorID)));
			}
			descriptors = ListUtil.toArray(desc);
		}
		return descriptors;
	}

	public synchronized static OBDescriptorProperty[] getDescriptorProps(DatasetFile dataset, boolean forceReload)
	{
		OBDescriptorPropertySet[] sets = getDescriptors(forceReload);
		OBDescriptorProperty[] props = new OBDescriptorProperty[sets.length];
		for (int i = 0; i < props.length; i++)
			props[i] = sets[i].createOBDescriptorProperty(dataset);
		return props;
	}

	private String descriptorID;
	private String description;
	private Type defaultType;

	private OBDescriptorPropertySet(String descriptorID, String description, Type defaultType)
	{
		this.descriptorID = descriptorID;
		this.description = description;
		this.defaultType = defaultType;
	}

	public String toString()
	{
		return descriptorID;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	private static DoubleKeyHashMap<String, DatasetFile, OBDescriptorProperty> props = new DoubleKeyHashMap<String, DatasetFile, OBDescriptorProperty>();

	private OBDescriptorProperty createOBDescriptorProperty(DatasetFile dataset)
	{
		if (!props.containsKeyPair(descriptorID, dataset))
			props.put(descriptorID, dataset, new OBDescriptorProperty(descriptorID, description, defaultType));
		return props.get(descriptorID, dataset);
	}

	public static OBDescriptorPropertySet fromString(String toString, Type type)
	{
		for (OBDescriptorPropertySet d : getDescriptors(false))
			if (d.toString().equals(toString))
			{
				if (type != null)
					d.defaultType = type;
				return d;
			}
		return null;
	}

	@Override
	public boolean isComputed(DatasetFile dataset)
	{
		return createOBDescriptorProperty(dataset).isValuesSet();
	}

	@Override
	public boolean isCached(DatasetFile dataset)
	{
		return new File(cacheFile(dataset)).exists();
	}

	private String cacheFile(DatasetFile dataset)
	{
		return dataset.getFeatureValuesFilePath(this);
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
			if (Settings.DESC_MIXTURE_HANDLING)
				vals = DescriptorForMixturesHandler.computeOBDescriptor(dataset, descriptorID);
			else
				vals = OBDescriptorFactory.compute(dataset.getSDF(), descriptorID);
			Settings.LOGGER.info("writing ob descriptors to: " + cache);
			ValueFileCache.writeCacheString(cache, vals);
		}

		OBDescriptorProperty prop = createOBDescriptorProperty(dataset);
		int numDistinct = -1;
		if (prop.isTypeAllowed(Type.NOMINAL))
		{
			prop.setStringValues(vals);
		}
		if (prop.isTypeAllowed(Type.NUMERIC))
		{
			Double dVals[] = ArrayUtil.parse(vals);
			if (dVals == null)
			{
				if (getType() == Type.NUMERIC)
				{
					TaskProvider.warning(
							"Cannot compute feature: " + prop.getName(),
							"Numeric features cannot be parsed. Values returned from OpenBabel:\n"
									+ ArrayUtil.toString(vals));
					new File(cache).delete();
					return false;
				}
				else
					prop.setTypeAllowed(Type.NUMERIC, false);
			}
			else
				prop.setDoubleValues(dVals);
		}
		if (prop.getType() == null)
		{
			if (prop.isTypeAllowed(Type.NOMINAL)
					&& FeatureService.guessNominalFeatureType(numDistinct, vals.length,
							prop.isTypeAllowed(Type.NUMERIC)))
				prop.setType(Type.NOMINAL);
			else if (prop.isTypeAllowed(Type.NUMERIC))
				prop.setType(Type.NUMERIC);
		}
		return true;
	}

	@Override
	public Type getType()
	{
		return defaultType;
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
	public CompoundProperty get(DatasetFile d, int index)
	{
		if (index != 0)
			throw new IllegalStateException();
		return createOBDescriptorProperty(d);
	}

	@Override
	public Binary getBinary()
	{
		return BinHandler.BABEL_BINARY;
	}

	@Override
	public boolean isSelectedForMapping()
	{
		return true;
	}

	@Override
	public String getNameIncludingParams()
	{
		String s = "OBDescriptor" + descriptorID + "_" + getType();
		if (Settings.DESC_MIXTURE_HANDLING)
			s += ".mixt";
		return s;
	}

	@Override
	public boolean isComputationSlow()
	{
		return false;
	}

	@Override
	public boolean isSensitiveTo3D()
	{
		return false;
	}

	@Override
	public SubstructureType getSubstructureType()
	{
		return null;
	}

}
