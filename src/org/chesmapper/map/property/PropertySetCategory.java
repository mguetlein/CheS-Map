package org.chesmapper.map.property;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.CompoundPropertySet;
import org.chesmapper.map.dataInterface.CompoundPropertySet.Type;
import org.chesmapper.map.main.PropHandler;
import org.chesmapper.map.main.Settings;
import org.mg.javalib.gui.binloc.Binary;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.ListUtil;
import org.mg.javalib.util.StringUtil;

public class PropertySetCategory
{
	private String name;
	private PropertySetCategory[] subCategory;

	PropertySetCategory(String name)
	{
		this(name, null);
	}

	PropertySetCategory(String name, PropertySetCategory[] subCategory)
	{
		this.name = name;
		this.subCategory = subCategory;
	}

	public String toString()
	{
		return Settings.text("features." + name);
	}

	public String getDescription()
	{
		return Settings.text("features." + name + ".desc", getDescriptionParam());
	}

	public String getDescriptionParam()
	{
		return "";
	}

	/**
	 * if sub-categories are null, the category has property sets
	 */
	public PropertySetCategory[] getSubCategory()
	{
		return subCategory;
	}

	/**
	 * returns all property sets that belong to this category (not to the subcategories)
	 */
	public CompoundPropertySet[] getPropertySet(DatasetFile dataset)
	{
		throw new IllegalStateException("please overwrite if no sub-categories added");
	}

	/**
	 * get props from shortcut only
	 */
	public CompoundPropertySet[] getPropertySet(DatasetFile dataset, PropertySetProvider.PropertySetShortcut shortcut)
	{
		return null;
	}

	/**
	 * if binary is required, a check is done in the selector (gui) accordingly
	 */
	public Binary getBinary()
	{
		return null;
	}

	/**
	 * to add the corresponding info in the gui if required
	 */
	public boolean isFragmentCategory()
	{
		return false;
	}

	/**
	 * to add the corresponding info in the gui if required 
	 */
	public boolean isSMARTSFragmentCategory()
	{
		return false;
	}

	/**
	 * de-serialize a property from the prop-file
	 */
	protected CompoundPropertySet fromString(String s, Type t, DatasetFile dataset)
	{
		throw new IllegalArgumentException("please overwrite");
	}

	/**
	 * key that is used to save selected compound-property-set in prop-file
	 */
	protected String getSerializeKey()
	{
		return null;
	}

	/**
	 * key that is used to typed, but un-selected compound-property-set in prop-file
	 */
	protected String getSerializeKeyType()
	{
		return null;
	}

	void putToProperties(CompoundPropertySet[] allSelectedProperties, Properties props, DatasetFile dataset)
	{
		if (getSerializeKey() != null)
		{
			CompoundPropertySet[] children = getPropertySetChildren(dataset);
			CompoundPropertySet[] selectedProperties = ArrayUtil.cut(CompoundPropertySet.class, children,
					allSelectedProperties);
			serialize(selectedProperties, getSerializeKey(), props);
			if (getSerializeKeyType() != null)
			{
				CompoundPropertySet[] unselectedProperties = ArrayUtil.remove(CompoundPropertySet.class, children,
						allSelectedProperties);
				if (unselectedProperties.length > 0)
					unselectedProperties = ArrayUtil.filter(CompoundPropertySet.class, unselectedProperties,
							new ListUtil.Filter<CompoundPropertySet>()
							{
								@Override
								public boolean accept(CompoundPropertySet c)
								{
									return c.getType() != null;
								}
							});
				if (unselectedProperties.length > 0)
					serialize(unselectedProperties, getSerializeKeyType(), props);
			}
		}
		else if (getSubCategory() == null)
			throw new IllegalArgumentException();
		else
			for (PropertySetCategory cat : getSubCategory())
				cat.putToProperties(allSelectedProperties, props, dataset);
	}

	private void serialize(CompoundPropertySet[] sets, String key, Properties props)
	{
		String[] selectedProps = new String[sets.length];
		for (int i = 0; i < selectedProps.length; i++)
			selectedProps[i] = sets[i].serialize();
		if (selectedProps.length == 0)
			props.remove(key);
		else
			props.put(key, ArrayUtil.toCSVString(selectedProps, true));
	}

	void exportToWorkflow(Properties workflow)
	{
		if (getSerializeKey() != null)
		{
			for (String k : new String[] { getSerializeKey(), getSerializeKeyType() })
			{
				if (k == null)
					continue;
				if (PropHandler.containsKey(k))
					workflow.put(k, PropHandler.get(k));
				else
					PropHandler.remove(k);
			}
		}
		else if (getSubCategory() == null)
			throw new IllegalArgumentException();
		else
			for (PropertySetCategory cat : getSubCategory())
				cat.exportToWorkflow(workflow);
	}

	CompoundPropertySet[] loadFromProperties(Properties props, boolean storeToSettings, DatasetFile dataset)
	{
		if (getSerializeKey() != null)
		{
			List<CompoundPropertySet> features = new ArrayList<CompoundPropertySet>();
			for (String k : new String[] { getSerializeKey(), getSerializeKeyType() })
			{
				if (k == null)
					continue;
				if (storeToSettings)
				{
					if (props.containsKey(k))
						PropHandler.put(k, (String) props.get(k));
					else
						PropHandler.remove(k);
				}
				List<String> selection = StringUtil.split((String) props.get(k));
				for (String string : selection)
				{
					if (string == null)
						continue;
					int index = string.lastIndexOf('#');
					Type t = null;
					if (index != -1)
					{
						try
						{
							t = Type.valueOf(string.substring(index + 1));
							string = string.substring(0, index);
						}
						catch (Exception e)
						{
						}
					}
					CompoundPropertySet d = fromString(string, t, dataset);
					if (k.equals(getSerializeKey()) && d != null)
						features.add(d);
				}
			}
			return ArrayUtil.toArray(CompoundPropertySet.class, features);
		}
		else if (getSubCategory() == null)
			throw new IllegalArgumentException();
		else
		{
			CompoundPropertySet res[] = new CompoundPropertySet[0];
			for (PropertySetCategory cat : getSubCategory())
				res = ArrayUtil.concat(CompoundPropertySet.class, res,
						cat.loadFromProperties(props, storeToSettings, dataset));
			return res;
		}
	}

	CompoundPropertySet[] getPropertySetChildren(DatasetFile dataset,
			PropertySetProvider.PropertySetShortcut... shortcuts)
	{
		if (getSubCategory() == null)
		{
			if (shortcuts == null || shortcuts.length == 0)
				return getPropertySet(dataset);
			else
			{
				CompoundPropertySet[] set = new CompoundPropertySet[0];
				for (PropertySetProvider.PropertySetShortcut k : shortcuts)
				{
					CompoundPropertySet[] p = getPropertySet(dataset, k);
					if (p != null)
						set = ArrayUtil.concat(CompoundPropertySet.class, set, p);
				}
				return set;
			}
		}
		else
		{
			CompoundPropertySet res[] = new CompoundPropertySet[0];
			for (PropertySetCategory cat : getSubCategory())
				res = ArrayUtil.concat(CompoundPropertySet.class, res, cat.getPropertySetChildren(dataset, shortcuts));
			return ArrayUtil.removeDuplicates(CompoundPropertySet.class, res);
		}
	}

	public void clearComputedProperties(DatasetFile dataset)
	{
		if (getSubCategory() == null)
		{
			for (CompoundPropertySet set : getPropertySet(dataset))
				set.clearComputedProperties(dataset);
		}
		else
		{
			for (PropertySetCategory cat : getSubCategory())
				cat.clearComputedProperties(dataset);
		}
	}

}
