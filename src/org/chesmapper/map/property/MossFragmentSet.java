package org.chesmapper.map.property;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.fragments.FragmentProperties;
import org.chesmapper.map.data.fragments.MatchEngine;
import org.chesmapper.map.dataInterface.DefaultFragmentProperty;
import org.chesmapper.map.dataInterface.FragmentPropertySet;
import org.chesmapper.map.dataInterface.FragmentProperty.SubstructureType;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.StringUtil;

import moss.Miner;
import moss.Miner.MinerAbortable;

public class MossFragmentSet extends FragmentPropertySet
{
	static final MossFragmentSet INSTANCE = new MossFragmentSet();

	public MossFragmentSet()
	{
		super(Settings.text("features.moss"), SubstructureType.MINE);
	}

	@Override
	protected void updateFragments()
	{
		for (DatasetFile d : minMinF.keySet())
		{
			int minMinFreq = minMinF.get(d);
			if (minMinFreq > FragmentProperties.getMinFrequency())
			{
				clearComputedProperties(d);
			}
			else if (!props.containsKey(d) && lastProps.containsKey(d))
			{
				props.put(d, lastProps.get(d));
			}
		}
		super.updateFragments();
	}

	protected HashMap<DatasetFile, Integer> minMinF = new HashMap<DatasetFile, Integer>();
	protected HashMap<DatasetFile, List<DefaultFragmentProperty>> lastProps = new HashMap<DatasetFile, List<DefaultFragmentProperty>>();

	private String getMossResultsFilePath(DatasetFile dataset)
	{
		return dataset.getFeatureValuesFilePath(this);
	}

	@Override
	public boolean isCached(DatasetFile dataset)
	{
		String path = getMossResultsFilePath(dataset);
		return Settings.CACHING_ENABLED && new File(path + "_1").exists() && new File(path + "_2").exists();
	}

	@Override
	public boolean compute(DatasetFile dataset)
	{
		boolean done = false;
		try
		{
			int intMinFreq = FragmentProperties.getMinFrequency();
			double minFreq = intMinFreq / (double) dataset.getCompounds().length * 100;
			//		if (minFreq > 5.0)
			//		{
			//			//5 procent is relatively fast to compute on all tested datasets
			//			minFreq = 5.0;
			//			intMinFreq = (int) (minFreq / 100.0 * dataset.getCompounds().length);
			//		}
			//		System.out.println(intMinFreq + " -> " + minFreq);

			String path = getMossResultsFilePath(dataset);
			String outfile1 = path + "_1";
			String outfile2 = path + "_2";

			if (!isCached(dataset))
			{

				StringBuffer smiString = new StringBuffer();
				int count = 1;
				for (String smiles : dataset.getSmiles())
				{
					smiString.append(count++);
					smiString.append(",0,");
					smiString.append(smiles);
					smiString.append("\n");
				}
				//		System.out.println(smiString.toString());
				String infile = File.createTempFile("moss", "smiles").getAbsolutePath();
				FileUtil.writeStringToFile(infile, smiString.toString());

				String cmd[] = new String[] { "-t0", "-ismi", "-s" + minFreq, infile, outfile1, outfile2 };
				//				Miner.main(cmd);

				Miner miner = new Miner();
				miner.setLog(new PrintStream(new FilterOutputStream(null))
				{
					@Override
					public void println(String x)
					{
						TaskProvider.debug("MoSS: " + x);
					}

					@Override
					public void print(String x)
					{
						if (!StringUtil.containsOnly(x, '\b'))
							TaskProvider.verbose("MoSS: " + x);
					}

					@Override
					public void println()
					{
					}
				});
				miner.init(cmd);
				miner.setAborter(new MinerAbortable()
				{
					@Override
					public boolean abort()
					{
						return !TaskProvider.isRunning();
					}
				});
				miner.run();
				if (!TaskProvider.isRunning())
					return false;
				if (miner.getError() != null)
					throw miner.getError();
				miner.stats();
				Settings.LOGGER.info("MoSS result saved to: " + outfile1);
			}
			else
			{
				Settings.LOGGER.info("MoSS result cached: " + outfile1);
			}
			if (!TaskProvider.isRunning())
				return false;
			String res1[] = FileUtil.readStringFromFile(outfile1).split("\n");
			String res2[] = FileUtil.readStringFromFile(outfile2).split("\n");
			if (res1.length != res2.length)
				throw new IllegalStateException("MoSS failed");
			if (res1.length == 0 || (res1.length == 1 && res1[0].trim().length() == 0))
				throw new IllegalStateException("MoSS failed");
			List<DefaultFragmentProperty> l = new ArrayList<DefaultFragmentProperty>();
			for (int i = 0; i < res1.length; i++)
			{
				if (i == 0)
					continue;
				//			System.out.println(res1[i]);
				//			System.out.println(res2[i]);
				String r1[] = res1[i].split(",");
				String r2[] = res2[i].split(",|:");
				//			System.out.println(ArrayUtil.toString(r1));
				//			System.out.println(ArrayUtil.toString(r2));
				if (!r1[0].equals(r2[0]))
					throw new IllegalStateException();
				String smiles = r1[1];
				DefaultFragmentProperty p = new DefaultFragmentProperty(this, smiles, "Structural Fragment", smiles,
						MatchEngine.OpenBabel);
				p.setFrequency(Integer.parseInt(r1[4]));
				String[] featureValue = new String[dataset.numCompounds()];
				Arrays.fill(featureValue, "0");
				for (int j = 1; j < r2.length; j++)
					featureValue[Integer.parseInt(r2[j]) - 1] = "1";
				p.setStringValues(featureValue);
				l.add(p);
			}
			props.put(dataset, l);
			if (!minMinF.containsKey(dataset) || minMinF.get(dataset) > intMinFreq)
			{
				minMinF.put(dataset, intMinFreq);
				lastProps.put(dataset, l);
			}
			done = true;
		}
		catch (Throwable e)
		{
			TaskProvider.warning("MoSS failed", e.getMessage());
			Settings.LOGGER.error(e);
			return false;
		}
		finally
		{
			if (!done && isCached(dataset))
			{
				String path = getMossResultsFilePath(dataset);
				new File(path + "_1").delete();
				new File(path + "_2").delete();
			}
		}
		updateFragments();
		return true;
	}

	@Override
	public boolean isSizeDynamicHigh(DatasetFile dataset)
	{
		return FragmentProperties.getMinFrequency() <= 10;
	}

	@Override
	public String getDescription()
	{
		return Settings.text("features.moss.desc", Settings.MOSS_VERSION);
	}

	@Override
	public boolean isComputationSlow()
	{
		return true;
	}

	@Override
	public boolean hasFixedMatchEngine()
	{
		return true;
	}
}
