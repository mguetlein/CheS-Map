package org.chesmapper.map.property;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.desc.DescriptorForMixturesHandler;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertySet;
import org.chesmapper.map.dataInterface.DefaultNominalProperty;
import org.chesmapper.map.dataInterface.DefaultNumericProperty;
import org.chesmapper.map.dataInterface.FragmentProperty.SubstructureType;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.chesmapper.map.util.ValueFileCache;
import org.mg.javalib.gui.binloc.Binary;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.ListUtil;
import org.mg.javalib.util.FileUtil.UnexpectedNumColsException;

public class OBDescriptorSet implements CompoundPropertySet
{
	private static OBDescriptorSet[] descriptors;
	private static HashMap<String, OBDescriptorSet> sets = new HashMap<String, OBDescriptorSet>();

	public static void loadDescriptors(boolean forceReload)
	{
		getDescriptors(forceReload);
	}

	synchronized static OBDescriptorSet[] getDescriptors(boolean forceReload)
	{
		if (descriptors == null || forceReload)
		{
			if (descriptors == null)
				descriptors = new OBDescriptorSet[0];

			Set<String> descriptorIDs = OBDescriptorFactory.getDescriptorIDs(forceReload);
			if (descriptorIDs == null)
				return new OBDescriptorSet[0];
			List<OBDescriptorSet> desc = new ArrayList<OBDescriptorSet>();
			for (String descriptorID : descriptorIDs)
			{
				OBDescriptorSet p = fromString(descriptorID, null);
				if (p != null)
					desc.add(p);
				else
					desc.add(new OBDescriptorSet(descriptorID, OBDescriptorFactory
							.getDescriptorDescription(descriptorID), OBDescriptorFactory
							.getDescriptorDefaultType(descriptorID)));
				sets.put(descriptorID, p);
			}
			descriptors = ListUtil.toArray(desc);
		}
		return descriptors;
	}

	synchronized static CompoundProperty[] getDescriptorProps(DatasetFile dataset, boolean forceReload)
	{
		OBDescriptorSet[] sets = getDescriptors(forceReload);
		CompoundProperty[] props = new CompoundProperty[sets.length];
		for (int i = 0; i < props.length; i++)
			props[i] = sets[i].createOBDescriptorProperty(dataset);
		return props;
	}

	private String descriptorID;
	private String description;
	private Type defaultType;

	private OBDescriptorSet(String descriptorID, String description, Type defaultType)
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

	@Override
	public void setTypeAllowed(Type type, boolean allowed)
	{
		throw new IllegalArgumentException();
	}

	@Override
	public void setType(Type type)
	{
		throw new IllegalArgumentException();
	}

	@Override
	public boolean isTypeAllowed(Type type)
	{
		return type == Type.NOMINAL;
	}

	@Override
	public Type getType()
	{
		return defaultType;
	}

	private HashMap<DatasetFile, CompoundProperty> props = new HashMap<DatasetFile, CompoundProperty>();

	@Override
	public void clearComputedProperties(DatasetFile d)
	{
		props.remove(d);
	}

	private CompoundProperty createOBDescriptorProperty(DatasetFile dataset)
	{
		if (!props.containsKey(dataset))
		{
			CompoundProperty p;
			if (getType() == Type.NUMERIC)
				p = new DefaultNumericProperty(this, descriptorID, description);
			else
				p = new DefaultNominalProperty(this, descriptorID, description);
			props.put(dataset, p);
		}
		return props.get(dataset);
	}

	@Override
	public String serialize()
	{
		return toString() + "#" + defaultType;
	}

	public static OBDescriptorSet fromString(String toString, Type type)
	{
		for (OBDescriptorSet d : getDescriptors(false))
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

		//		int numDistinct = -1;
		if (getType() == Type.NOMINAL)
		{
			((DefaultNominalProperty) createOBDescriptorProperty(dataset)).setStringValues(vals);
		}
		else if (getType() == Type.NUMERIC)
		{
			DefaultNumericProperty prop = (DefaultNumericProperty) createOBDescriptorProperty(dataset);
			Double dVals[] = ArrayUtil.parse(vals);
			if (dVals == null)
			{
				TaskProvider.warning(
						"Cannot compute feature: " + prop.getName(),
						"Numeric features cannot be parsed. Values returned from OpenBabel:\n"
								+ ArrayUtil.toString(vals));
				new File(cache).delete();
				return false;
			}
			else
				prop.setDoubleValues(dVals);
		}
		else
			throw new IllegalStateException();
		//		if (prop.getType() == null)
		//		{
		//			if (prop.isTypeAllowed(Type.NOMINAL)
		//					&& FeatureService.guessNominalFeatureType(numDistinct, vals.length,
		//							prop.isTypeAllowed(Type.NUMERIC)))
		//				prop.setType(Type.NOMINAL);
		//			else if (prop.isTypeAllowed(Type.NUMERIC))
		//				prop.setType(Type.NUMERIC);
		//		}
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
		throw new IllegalStateException();
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

	@Override
	public boolean isSmiles()
	{
		return false;
	}

	@Override
	public void setSmiles(boolean smiles)
	{
		throw new IllegalStateException();
	}

	@Override
	public boolean isHiddenFromGUI()
	{
		return false;
	}

}
