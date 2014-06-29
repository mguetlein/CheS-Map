package dataInterface;

import gui.binloc.Binary;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import data.DatasetFile;
import data.fragments.StructuralFragmentProperties;
import dataInterface.CompoundProperty.SubstructureType;
import dataInterface.CompoundProperty.Type;

public abstract class FragmentPropertySet implements CompoundPropertySet
{
	protected String name;
	protected SubstructureType substructureType;

	public FragmentPropertySet(String name, SubstructureType substructureType)
	{
		this();
		this.name = name;
		this.substructureType = substructureType;
	}

	public FragmentPropertySet()
	{
		StructuralFragmentProperties.addPropertyChangeListenerToProperties(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				updateFragments();
			}
		});
	}

	public String toString()
	{
		return name;
	}

	@Override
	public boolean isSensitiveTo3D()
	{
		return false;
	}

	protected HashMap<DatasetFile, List<FragmentProperty>> props = new HashMap<DatasetFile, List<FragmentProperty>>();
	private HashMap<DatasetFile, List<FragmentProperty>> filteredProps = new HashMap<DatasetFile, List<FragmentProperty>>();

	@Override
	public int getSize(DatasetFile d)
	{
		if (filteredProps.get(d) == null)
			throw new Error("mine fragments first, number is not fixed");
		return filteredProps.get(d).size();
	}

	@Override
	public FragmentProperty get(DatasetFile d, int index)
	{
		if (filteredProps.get(d) == null)
			throw new Error("mine fragments first, number is not fixed");
		return filteredProps.get(d).get(index);
	}

	@Override
	public boolean isComputed(DatasetFile dataset)
	{
		return filteredProps.get(dataset) != null;
	}

	@Override
	public boolean isSizeDynamic()
	{
		return true;
	}

	protected void updateFragments()
	{
		for (DatasetFile d : props.keySet())
		{
			List<FragmentProperty> filteredList = new ArrayList<FragmentProperty>();
			for (FragmentProperty p : props.get(d))
			{
				boolean frequent = p.getFrequency() >= StructuralFragmentProperties.getMinFrequency();
				boolean skipOmni = StructuralFragmentProperties.isSkipOmniFragments()
						&& p.getFrequency() == d.numCompounds();
				if (frequent && !skipOmni)
					filteredList.add(p);
			}
			filteredProps.put(d, filteredList);
		}
	}

	@Override
	public Type getType()
	{
		return Type.NOMINAL;
	}

	@Override
	public boolean isSelectedForMapping()
	{
		return true;
	}

	@Override
	public String getNameIncludingParams()
	{
		return toString() + "_" + StructuralFragmentProperties.getMatchEngine() + "_"
				+ StructuralFragmentProperties.getMinFrequency() + "_"
				+ StructuralFragmentProperties.isSkipOmniFragments();
	}

	@Override
	public boolean isCached(DatasetFile dataset)
	{
		return false;
	}

	@Override
	public Binary getBinary()
	{
		return null;
	}

	@Override
	public SubstructureType getSubstructureType()
	{
		return substructureType;
	}

	//	public void clearProperties()
	//	{
	//		props.clear();
	//		filteredProps.clear();
	//	}

}
