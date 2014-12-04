package org.chesmapper.map.dataInterface;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.fragments.FragmentProperties;
import org.chesmapper.map.dataInterface.FragmentProperty.SubstructureType;
import org.mg.javalib.gui.binloc.Binary;

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
		FragmentProperties.addPropertyChangeListenerToProperties(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				updateFragments();
			}
		}, hasFixedMatchEngine());
	}

	public String toString()
	{
		return name;
	}

	@Override
	public String serialize()
	{
		return toString();
	}

	@Override
	public boolean isSensitiveTo3D()
	{
		return false;
	}

	protected HashMap<DatasetFile, List<DefaultFragmentProperty>> props = new HashMap<DatasetFile, List<DefaultFragmentProperty>>();
	private HashMap<DatasetFile, List<DefaultFragmentProperty>> filteredProps = new HashMap<DatasetFile, List<DefaultFragmentProperty>>();

	@Override
	public void clearComputedProperties(DatasetFile d)
	{
		props.remove(d);
		filteredProps.remove(d);
	}

	@Override
	public int getSize(DatasetFile d)
	{
		if (filteredProps.get(d) == null)
			throw new Error("mine fragments first, number is not fixed");
		return filteredProps.get(d).size();
	}

	@Override
	public DefaultFragmentProperty get(DatasetFile d, int index)
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
			List<DefaultFragmentProperty> filteredList = new ArrayList<DefaultFragmentProperty>();
			for (DefaultFragmentProperty p : props.get(d))
			{
				boolean frequent = p.getFrequency() >= FragmentProperties.getMinFrequency();
				boolean skipOmni = FragmentProperties.isSkipOmniFragments() && p.getFrequency() == d.numCompounds();
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
	public final String getNameIncludingParams()
	{
		return toString().replace(" ", "-") + (hasFixedMatchEngine() ? "" : "_" + FragmentProperties.getMatchEngine())
				+ "_" + FragmentProperties.getMinFrequency() + "_" + FragmentProperties.isSkipOmniFragments();
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

	@Override
	public void setType(Type type)
	{
		//		if (type != Type.NOMINAL)
		throw new IllegalStateException();
	}

	@Override
	public boolean isTypeAllowed(Type type)
	{
		return type == Type.NOMINAL;
	}

	@Override
	public void setTypeAllowed(Type type, boolean allowed)
	{
		throw new IllegalStateException();
	}

	@Override
	public void setSmiles(boolean smiles)
	{
		throw new IllegalStateException();
	}

	@Override
	public boolean isSmiles()
	{
		return false;
	}

	@Override
	public boolean isHiddenFromGUI()
	{
		return false;
	}

	public abstract boolean hasFixedMatchEngine();

	//	public void clearProperties()
	//	{
	//		props.clear();
	//		filteredProps.clear();
	//	}

}
