package data.fminer;

import gui.binloc.Binary;
import gui.binloc.BinaryLocator;

import java.util.ArrayList;
import java.util.List;

import main.BinHandler;
import main.Settings;
import util.ArrayUtil;
import util.FminerWrapper;
import data.DatasetFile;
import data.fragments.MatchEngine;
import data.fragments.StructuralFragmentProperties;
import dataInterface.CompoundProperty.SubstructureType;
import dataInterface.FragmentProperty;
import dataInterface.FragmentPropertySet;

public class FminerPropertySet extends FragmentPropertySet
{
	public FminerPropertySet()
	{
		super("fminer", SubstructureType.MINE);
	}

	//	@Override
	//	public String toString()
	//	{
	//		return name;
	//	}

	//	@Override
	//	public boolean isComputed(DatasetFile dataset)
	//	{
	//		return props.get(dataset) != null;
	//	}

	//	@Override
	//	public boolean isCached(DatasetFile dataset)
	//	{
	//		return false;
	//	}
	//
	//	private HashMap<DatasetFile, List<FminerProperty>> props = new HashMap<DatasetFile, List<FminerProperty>>();
	//	private HashMap<DatasetFile, List<FminerProperty>> filteredProps = new HashMap<DatasetFile, List<FminerProperty>>();

	@Override
	public boolean compute(DatasetFile dataset)
	{
		System.err.println("REMOVE FMINER LOCATE WHEN ADDED TO RELEASE AND TO OTHER BINARIES");
		BinaryLocator.locate(BinHandler.FMINER_BINARY);
		if (!BinHandler.FMINER_BINARY.isFound())
			throw new Error("fminer not found at " + System.getenv().get(BinHandler.FMINER_BINARY.getEnvName()));
		try
		{
			FminerWrapper fw = new FminerWrapper(Settings.LOGGER, BinHandler.FMINER_BINARY.getLocation(),
					BinHandler.FMINER_BINARY.getBBRCLib(), System.getenv().get("LD_LIBRARY_PATH"),
					BinHandler.BABEL_BINARY.getLocation(), dataset.getSmiles(),
					StructuralFragmentProperties.getMinFrequency());
			List<FragmentProperty> properties = new ArrayList<FragmentProperty>();
			for (int i = 0; i < fw.getSmarts().length; i++)
			{
				FragmentProperty p = new FragmentProperty(this, fw.getSmarts()[i], "Structural Fragment",
						fw.getSmarts()[i], MatchEngine.OpenBabel);
				//				FminerProperty p = FminerProperty.create(this, fw.getSmarts()[i]);

				Boolean h[] = ArrayUtil.toBooleanArray(fw.getHitsForSmarts(i));

				p.setFrequency(ArrayUtil.occurences(h, Boolean.TRUE));

				String[] featureValue = new String[dataset.numCompounds()];
				for (int j = 0; j < dataset.numCompounds(); j++)
					featureValue[j] = h[j] ? "1" : "0";
				//				featureValues.add(featureValue);
				p.setStringValues(featureValue);

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

	//	@Override
	//	public boolean isSizeDynamic()
	//	{
	//		return true;
	//	}

	@Override
	public boolean isSizeDynamicHigh(DatasetFile dataset)
	{
		return true;
	}

	//	@Override
	//	public SubstructureType getSubstructureType()
	//	{
	//		return SubstructureType.MINE;
	//	}

	//	@Override
	//	public int getSize(DatasetFile d)
	//	{
	//		if (filteredProps.get(d) == null)
	//			throw new Error("mine fragments first, number is not fixed");
	//		return filteredProps.get(d).size();
	//	}
	//
	//	@Override
	//	public CompoundProperty get(DatasetFile d, int index)
	//	{
	//		if (filteredProps.get(d) == null)
	//			throw new Error("mine fragments first, number is not fixed");
	//		return filteredProps.get(d).get(index);
	//	}

	@Override
	public String getDescription()
	{
		return "yet to come";
	}

	//	@Override
	//	public Type getType()
	//	{
	//		return Type.NOMINAL;
	//	}

	@Override
	public Binary getBinary()
	{
		return BinHandler.FMINER_BINARY;
	}

	//	@Override
	//	public boolean isSelectedForMapping()
	//	{
	//		return true;
	//	}

	@Override
	public String getNameIncludingParams()
	{
		return "TODO";
	}

	@Override
	public boolean isComputationSlow()
	{
		return false;
	}

	//	@Override
	//	protected void updateFragments()
	//	{
	//		for (DatasetFile d : props.keySet())
	//		{
	//			List<FminerProperty> filteredList = new ArrayList<FminerProperty>();
	//			for (FminerProperty p : props.get(d))
	//			{
	//				boolean frequent = p.getFrequency() >= StructuralFragmentProperties.getMinFrequency();
	//				boolean skipOmni = StructuralFragmentProperties.isSkipOmniFragments()
	//						&& p.getFrequency() == d.numCompounds();
	//				if (frequent && !skipOmni)
	//					filteredList.add(p);
	//			}
	//			filteredProps.put(d, filteredList);
	//		}
	//	}

}
