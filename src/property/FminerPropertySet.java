package property;

import gui.binloc.Binary;
import gui.binloc.BinaryLocator;

import java.util.ArrayList;
import java.util.List;

import main.BinHandler;
import main.Settings;
import util.ArrayUtil;
import util.FminerWrapper;
import data.DatasetFile;
import data.fragments.FragmentProperties;
import data.fragments.MatchEngine;
import dataInterface.DefaultFragmentProperty;
import dataInterface.FragmentProperty.SubstructureType;
import dataInterface.FragmentPropertySet;

public class FminerPropertySet extends FragmentPropertySet
{
	static FminerPropertySet INSTANCE = new FminerPropertySet();

	private FminerPropertySet()
	{
		super("fminer", SubstructureType.MINE);
	}

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
					BinHandler.BABEL_BINARY.getLocation(), dataset.getSmiles(), FragmentProperties.getMinFrequency());
			List<DefaultFragmentProperty> properties = new ArrayList<DefaultFragmentProperty>();
			for (int i = 0; i < fw.getSmarts().length; i++)
			{
				DefaultFragmentProperty p = new DefaultFragmentProperty(this, fw.getSmarts()[i], "Structural Fragment",
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

	@Override
	public String getDescription()
	{
		return "yet to come";
	}

	@Override
	public Binary getBinary()
	{
		return BinHandler.FMINER_BINARY;
	}

	@Override
	public boolean isComputationSlow()
	{
		return false;
	}

	@Override
	public boolean isHiddenFromGUI()
	{
		return true;
	}

	@Override
	public boolean hasFixedMatchEngine()
	{
		return true;
	}

	@Override
	public boolean isSizeDynamicHigh(DatasetFile dataset)
	{
		return false;
	}
}
