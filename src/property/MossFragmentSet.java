package property;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import main.Settings;
import moss.Miner;
import util.FileUtil;
import data.DatasetFile;
import data.fragments.FragmentProperties;
import data.fragments.MatchEngine;
import dataInterface.DefaultFragmentProperty;
import dataInterface.FragmentProperty.SubstructureType;
import dataInterface.FragmentPropertySet;

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
		return new File(path + "_1").exists() && new File(path + "_2").exists();
	}

	@Override
	public boolean compute(DatasetFile dataset)
	{
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
				Miner.main(cmd);

				Settings.LOGGER.info("Moss result saved to: " + outfile1);
			}
			else
			{
				Settings.LOGGER.info("Moss result cached: " + outfile1);
			}
			String res1[] = FileUtil.readStringFromFile(outfile1).split("\n");
			String res2[] = FileUtil.readStringFromFile(outfile2).split("\n");
			if (res1.length != res2.length)
				throw new IllegalStateException();
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

			updateFragments();
			return true;
		}
		catch (Exception e)
		{
			Settings.LOGGER.error(e);
			return false;
		}
	}

	@Override
	public boolean isSizeDynamicHigh(DatasetFile dataset)
	{
		return true;
	}

	@Override
	public String getDescription()
	{
		return Settings.text("features.moss.desc", Settings.MOSS_VERSION);
	}

	@Override
	public boolean isComputationSlow()
	{
		return false;
	}

	@Override
	public boolean hasFixedMatchEngine()
	{
		return true;
	}
}
