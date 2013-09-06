package data.fminer;

import gui.binloc.Binary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.BinHandler;
import main.Settings;
import util.ArrayUtil;
import util.FminerWrapper;
import data.DatasetFile;
import data.fragments.StructuralFragmentProperties;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;
import dataInterface.FragmentPropertySet;

public class FminerPropertySet extends FragmentPropertySet
{
	String name = "fminer";

	@Override
	public String toString()
	{
		return name;
	}

	@Override
	public boolean isComputed(DatasetFile dataset)
	{
		return props.get(dataset) != null;
	}

	@Override
	public boolean isCached(DatasetFile dataset)
	{
		return false;
	}

	private HashMap<DatasetFile, List<FminerProperty>> props = new HashMap<DatasetFile, List<FminerProperty>>();
	private HashMap<DatasetFile, List<FminerProperty>> filteredProps = new HashMap<DatasetFile, List<FminerProperty>>();

	@Override
	public boolean compute(DatasetFile dataset)
	{
		try
		{
			FminerWrapper fw = new FminerWrapper(Settings.LOGGER, "/home/martin/software/fminer2/fminer/fminer",
					"/home/martin/software/fminer2/libbbrc/libbbrc.so", BinHandler.BABEL_BINARY.getLocation(),
					dataset.getSmiles(), StructuralFragmentProperties.getMinFrequency());
			List<FminerProperty> properties = new ArrayList<FminerProperty>();
			for (int i = 0; i < fw.getSmarts().length; i++)
			{
				FminerProperty p = FminerProperty.create(this, fw.getSmarts()[i]);

				Boolean h[] = ArrayUtil.toBooleanArray(fw.getHitsForSmarts(i));

				p.setFrequency(dataset, ArrayUtil.occurences(h, Boolean.TRUE));

				String[] featureValue = new String[dataset.numCompounds()];
				for (int j = 0; j < dataset.numCompounds(); j++)
					featureValue[j] = h[j] ? "1" : "0";
				//				featureValues.add(featureValue);
				p.setStringValues(dataset, featureValue);

				properties.add(p);
			}
			props.put(dataset, properties);
			updateFragments();
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean isSizeDynamic()
	{
		return true;
	}

	@Override
	public boolean isSizeDynamicHigh(DatasetFile dataset)
	{
		return true;
	}

	@Override
	public int getSize(DatasetFile d)
	{
		if (filteredProps.get(d) == null)
			throw new Error("mine fragments first, number is not fixed");
		return filteredProps.get(d).size();
	}

	@Override
	public CompoundProperty get(DatasetFile d, int index)
	{
		if (filteredProps.get(d) == null)
			throw new Error("mine fragments first, number is not fixed");
		return filteredProps.get(d).get(index);
	}

	@Override
	public String getDescription()
	{
		return "yet to come";
	}

	@Override
	public Type getType()
	{
		return Type.NOMINAL;
	}

	@Override
	public Binary getBinary()
	{
		return null;
	}

	@Override
	public boolean isUsedForMapping()
	{
		return true;
	}

	@Override
	public String getNameIncludingParams()
	{
		// TODO
		return "todo";
	}

	@Override
	public boolean isComputationSlow()
	{
		return false;
	}

	@Override
	protected void updateFragments()
	{
		for (DatasetFile d : props.keySet())
		{
			List<FminerProperty> filteredList = new ArrayList<FminerProperty>();
			for (FminerProperty p : props.get(d))
			{
				boolean frequent = p.getFrequency(d) >= StructuralFragmentProperties.getMinFrequency();
				boolean skipOmni = StructuralFragmentProperties.isSkipOmniFragments()
						&& p.getFrequency(d) == d.numCompounds();
				if (frequent && !skipOmni)
					filteredList.add(p);
			}
			filteredProps.put(d, filteredList);
		}
	}

}
