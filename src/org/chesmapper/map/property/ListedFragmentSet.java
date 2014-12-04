package org.chesmapper.map.property;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.chesmapper.map.data.CDKSmartsHandler;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.OpenBabelSmartsHandler;
import org.chesmapper.map.data.fragments.FragmentProperties;
import org.chesmapper.map.data.fragments.MatchEngine;
import org.chesmapper.map.dataInterface.FragmentPropertySet;
import org.chesmapper.map.dataInterface.FragmentProperty.SubstructureType;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.mg.javalib.gui.binloc.Binary;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.DoubleKeyHashMap;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.FileUtil.CSVFile;
import org.mg.javalib.util.FileUtil.UnexpectedNumColsException;

public class ListedFragmentSet extends FragmentPropertySet
{
	String description;

	HashMap<MatchEngine, List<ListedFragmentProperty>> fragments;
	DoubleKeyHashMap<MatchEngine, DatasetFile, String> cacheFile = new DoubleKeyHashMap<MatchEngine, DatasetFile, String>();
	DoubleKeyHashMap<MatchEngine, DatasetFile, List<ListedFragmentProperty>> computedFragments = new DoubleKeyHashMap<MatchEngine, DatasetFile, List<ListedFragmentProperty>>();

	ListedFragmentSet(String name, String description, HashMap<MatchEngine, List<ListedFragmentProperty>> fragments)
	{
		super(name, SubstructureType.MATCH);
		this.fragments = fragments;
		this.description = description;

		for (MatchEngine m : fragments.keySet())
			for (ListedFragmentProperty fragment : fragments.get(m))
				fragment.setListedFragmentSet(this);
	}

	@Override
	public void clearComputedProperties(DatasetFile d)
	{
		super.clearComputedProperties(d);
		cacheFile.removeWithKey2(d);
		computedFragments.removeWithKey2(d);
	}

	public String toString()
	{
		return name;
	}

	@Override
	public int getSize(DatasetFile d)
	{
		if (computedFragments.get(FragmentProperties.getMatchEngine(), d) == null)
			throw new Error("mine " + this + " fragments first, number is not fixed");
		return computedFragments.get(FragmentProperties.getMatchEngine(), d).size();
	}

	@Override
	public ListedFragmentProperty get(DatasetFile d, int index)
	{
		if (computedFragments.get(FragmentProperties.getMatchEngine(), d) == null)
			throw new Error("mine " + this + " fragments first, number is not fixed");
		return computedFragments.get(FragmentProperties.getMatchEngine(), d).get(index);
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
		if (FragmentProperties.getMatchEngine() == MatchEngine.OpenBabel)
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
				for (ListedFragmentProperty a : fragments.get(m))
					addFragment(a, d, m);
			}
		}
	}

	private void addFragment(ListedFragmentProperty a, DatasetFile d, MatchEngine m)
	{
		boolean skip = FragmentProperties.isSkipOmniFragments() && a.getFrequency() == d.numCompounds();
		boolean frequent = a.getFrequency() >= FragmentProperties.getMinFrequency();
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
		return computedFragments.get(FragmentProperties.getMatchEngine(), dataset) != null;
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

	private List<boolean[]> readFromFile(String file, int expectedNumRows) throws UnexpectedNumColsException
	{
		CSVFile f = FileUtil.readCSV(file, expectedNumRows);
		List<boolean[]> l = new ArrayList<boolean[]>();
		for (String[] s : f.content)
			l.add(ArrayUtil.parseBoolean(s));
		return l;
	}

	private String getSmartsMatchCacheFile(DatasetFile dataset)
	{
		if (cacheFile.get(FragmentProperties.getMatchEngine(), dataset) == null)
		{
			cacheFile.put(
					FragmentProperties.getMatchEngine(),
					dataset,
					dataset.getSmartsMatchesFilePath(FragmentProperties.getMatchEngine(),
							fragments.get(FragmentProperties.getMatchEngine())));
		}
		return cacheFile.get(FragmentProperties.getMatchEngine(), dataset);
	}

	@Override
	public boolean isCached(DatasetFile dataset)
	{
		return new File(getSmartsMatchCacheFile(dataset)).exists();
	}

	@Override
	public boolean compute(DatasetFile dataset)
	{
		Settings.LOGGER.info("Computing structural fragment " + FragmentProperties.getMatchEngine() + " "
				+ FragmentProperties.getMinFrequency() + " " + FragmentProperties.isSkipOmniFragments() + " "
				+ dataset.getSDF());

		List<String> smarts = new ArrayList<String>();
		for (ListedFragmentProperty fragment : fragments.get(FragmentProperties.getMatchEngine()))
			smarts.add(fragment.getSmarts());
		List<Integer> minNumMatches = new ArrayList<Integer>();
		for (ListedFragmentProperty fragment : fragments.get(FragmentProperties.getMatchEngine()))
			minNumMatches.add(fragment.getMinNumMatches());

		String smartsMatchFile = getSmartsMatchCacheFile(dataset);

		List<boolean[]> matches = null;
		if (Settings.CACHING_ENABLED && new File(smartsMatchFile).exists())
		{
			Settings.LOGGER.info("Read cached matches from file: " + smartsMatchFile);
			try
			{
				matches = readFromFile(smartsMatchFile, dataset.numCompounds());
			}
			catch (UnexpectedNumColsException e)
			{
				Settings.LOGGER.error(e);
			}
		}
		if (matches == null)
		{
			if (FragmentProperties.getMatchEngine() == MatchEngine.CDK)
				matches = new CDKSmartsHandler().match(smarts, minNumMatches, dataset);
			else if (FragmentProperties.getMatchEngine() == MatchEngine.OpenBabel)
				matches = new OpenBabelSmartsHandler().match(smarts, minNumMatches, dataset);
			else
				throw new Error("illegal match engine");
			if (!TaskProvider.isRunning())
				return false;
			Settings.LOGGER.info("store matches in file: " + smartsMatchFile);
			writeToFile(smartsMatchFile, matches);
		}

		//		if (!computedFragments.containsKeyPair(StructuralFragmentProperties.getMatchEngine(), dataset))
		computedFragments.put(FragmentProperties.getMatchEngine(), dataset, new ArrayList<ListedFragmentProperty>());

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
			ListedFragmentProperty fragment = fragments.get(FragmentProperties.getMatchEngine()).get(count);
			fragment.setFrequency(f);
			fragment.setStringValues(m);
			addFragment(fragment, dataset, FragmentProperties.getMatchEngine());
			count++;
		}
		return true;
	}

	@Override
	public boolean isSelectedForMapping()
	{
		return true;
	}

	@Override
	public boolean isComputationSlow()
	{
		return FragmentProperties.getMatchEngine() == MatchEngine.CDK && fragments.size() > 1000;
	}

	@Override
	public boolean hasFixedMatchEngine()
	{
		return false;
	}

	@Override
	public boolean isSizeDynamicHigh(DatasetFile dataset)
	{
		return false;
	}
}