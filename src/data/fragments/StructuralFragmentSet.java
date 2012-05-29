package data.fragments;

import gui.binloc.Binary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.BinHandler;
import main.Settings;
import main.TaskProvider;
import util.ArrayUtil;
import util.DoubleKeyHashMap;
import util.FileUtil;
import util.FileUtil.CSVFile;
import util.StringUtil;
import data.CDKSmartsHandler;
import data.DatasetFile;
import data.OpenBabelSmartsHandler;
import dataInterface.FragmentPropertySet;
import dataInterface.MoleculeProperty.Type;

public class StructuralFragmentSet extends FragmentPropertySet
{
	String description;
	String name;

	HashMap<MatchEngine, List<StructuralFragment>> fragments;
	DoubleKeyHashMap<MatchEngine, DatasetFile, String> cacheFile = new DoubleKeyHashMap<MatchEngine, DatasetFile, String>();
	DoubleKeyHashMap<MatchEngine, DatasetFile, List<StructuralFragment>> computedFragments = new DoubleKeyHashMap<MatchEngine, DatasetFile, List<StructuralFragment>>();

	public StructuralFragmentSet(String name, String description,
			HashMap<MatchEngine, List<StructuralFragment>> fragments)
	{
		this.fragments = fragments;
		this.description = description;
		this.name = name;

		for (MatchEngine m : fragments.keySet())
			for (StructuralFragment fragment : fragments.get(m))
				fragment.set = this;
	}

	public String toString()
	{
		return name;
	}

	@Override
	public int getSize(DatasetFile d)
	{
		if (computedFragments.get(StructuralFragmentProperties.getMatchEngine(), d) == null)
			throw new Error("mine " + this + " fragments first, number is not fixed");
		return computedFragments.get(StructuralFragmentProperties.getMatchEngine(), d).size();
	}

	@Override
	public StructuralFragment get(DatasetFile d, int index)
	{
		if (computedFragments.get(StructuralFragmentProperties.getMatchEngine(), d) == null)
			throw new Error("mine " + this + " fragments first, number is not fixed");
		return computedFragments.get(StructuralFragmentProperties.getMatchEngine(), d).get(index);
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public Type getType()
	{
		return Type.NOMINAL;
	}

	@Override
	public Binary getBinary()
	{
		if (StructuralFragmentProperties.getMatchEngine() == MatchEngine.OpenBabel)
			return BinHandler.BABEL_BINARY;
		else
			return null;
	}

	@Override
	protected void updateFragments()
	{
		for (MatchEngine m : computedFragments.keySet1())
		{
			for (DatasetFile d : computedFragments.keySet2(m))
			{
				computedFragments.get(m, d).clear();
				for (StructuralFragment a : fragments.get(m))
					addFragment(a, d, m);
			}
		}
	}

	private void addFragment(StructuralFragment a, DatasetFile d, MatchEngine m)
	{
		boolean skip = StructuralFragmentProperties.isSkipOmniFragments() && a.getFrequency(d) == d.numCompounds();
		boolean frequent = a.getFrequency(d) >= StructuralFragmentProperties.getMinFrequency();
		if (!skip && frequent)
			computedFragments.get(m, d).add(a);
	}

	@Override
	public boolean isSizeDynamic()
	{
		return true;
	}

	@Override
	public boolean isComputed(DatasetFile dataset)
	{
		return computedFragments.get(StructuralFragmentProperties.getMatchEngine(), dataset) != null;
	}

	private void writeToFile(String file, List<boolean[]> matches)
	{
		File f = new File(file);
		try
		{
			BufferedWriter b = new BufferedWriter(new FileWriter(f));
			for (boolean[] bb : matches)
			{
				b.write(ArrayUtil.booleanToCSVString(bb) + "\n");
			}
			b.close();
		}
		catch (IOException e)
		{
			throw new Error(e);
		}
	}

	private List<boolean[]> readFromFile(String file)
	{
		CSVFile f = FileUtil.readCSV(file);
		List<boolean[]> l = new ArrayList<boolean[]>();
		for (String[] s : f.content)
			l.add(ArrayUtil.parseBoolean(s));
		return l;
	}

	private String getSmartsMatchCacheFile(DatasetFile dataset)
	{
		if (cacheFile.get(StructuralFragmentProperties.getMatchEngine(), dataset) == null)
		{
			String allSmartsStrings = "";
			for (StructuralFragment fragment : fragments.get(StructuralFragmentProperties.getMatchEngine()))
				allSmartsStrings += fragment.getSmarts();
			String enc = StringUtil.getMD5(allSmartsStrings + dataset.getMD5());
			cacheFile.put(
					StructuralFragmentProperties.getMatchEngine(),
					dataset,
					Settings.destinationFile(dataset,
							StructuralFragmentProperties.getMatchEngine() + "." + dataset.getShortName() + "." + enc
									+ ".matches.csv"));
		}
		return cacheFile.get(StructuralFragmentProperties.getMatchEngine(), dataset);
	}

	@Override
	public boolean isCached(DatasetFile dataset)
	{
		return new File(getSmartsMatchCacheFile(dataset)).exists();
	}

	@Override
	public boolean compute(DatasetFile dataset)
	{
		Settings.LOGGER.info("computing structural fragment " + StructuralFragmentProperties.getMatchEngine() + " "
				+ StructuralFragmentProperties.getMinFrequency() + " "
				+ StructuralFragmentProperties.isSkipOmniFragments() + " " + dataset.getSDFPath(false));

		List<String> smarts = new ArrayList<String>();
		for (StructuralFragment fragment : fragments.get(StructuralFragmentProperties.getMatchEngine()))
			smarts.add(fragment.getSmarts());

		String smartsMatchFile = getSmartsMatchCacheFile(dataset);

		List<boolean[]> matches;
		if (Settings.CACHING_ENABLED && new File(smartsMatchFile).exists())
		{
			Settings.LOGGER.info("read cached matches from file: " + smartsMatchFile);
			matches = readFromFile(smartsMatchFile);
		}
		else
		{
			if (StructuralFragmentProperties.getMatchEngine() == MatchEngine.CDK)
				matches = new CDKSmartsHandler().match(smarts, dataset);
			else if (StructuralFragmentProperties.getMatchEngine() == MatchEngine.OpenBabel)
				matches = new OpenBabelSmartsHandler().match(smarts, dataset);
			else
				throw new Error("illegal match engine");
			if (!TaskProvider.isRunning())
				return false;
			Settings.LOGGER.info("store matches in file: " + smartsMatchFile);
			writeToFile(smartsMatchFile, matches);
		}

		//		if (!computedFragments.containsKeyPair(StructuralFragmentProperties.getMatchEngine(), dataset))
		computedFragments.put(StructuralFragmentProperties.getMatchEngine(), dataset,
				new ArrayList<StructuralFragment>());

		int count = 0;
		for (boolean[] match : matches)
		{
			String m[] = new String[match.length];
			int f = 0;
			for (int i = 0; i < m.length; i++)
			{
				if (match[i])
				{
					m[i] = "1";
					f++;
				}
				else
					m[i] = "0";
			}
			StructuralFragment fragment = fragments.get(StructuralFragmentProperties.getMatchEngine()).get(count);
			fragment.setNominalDomain(new String[] { "0", "1" });
			fragment.setFrequency(dataset, f);
			fragment.setStringValues(dataset, m);
			addFragment(fragment, dataset, StructuralFragmentProperties.getMatchEngine());
			count++;
		}
		return true;
	}

	@Override
	public boolean isUsedForMapping()
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
	public boolean isSizeDynamicHigh(DatasetFile dataset)
	{
		return false;
	}

	@Override
	public boolean isComputationSlow()
	{
		return StructuralFragmentProperties.getMatchEngine() == MatchEngine.CDK && fragments.size() > 1000;
	}
}