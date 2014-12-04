package org.chesmapper.map.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.fragments.MatchEngine;
import org.chesmapper.map.dataInterface.DefaultFragmentProperty;
import org.chesmapper.map.dataInterface.FragmentPropertySet;
import org.chesmapper.map.dataInterface.FragmentProperty.SubstructureType;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.CountedSet;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.fingerprint.KlekotaRothFingerprinter;
import org.openscience.cdk.fingerprint.StandardSubstructureSets;
import org.openscience.cdk.fingerprint.SubstructureFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;

public class CDKFingerprintSet extends FragmentPropertySet
{
	static final CDKFingerprintSet[] FINGERPRINTS = new CDKFingerprintSet[2];
	static CDKFingerprintSet FUNCTIONAL_GROUPS;
	static
	{
		try
		{
			FUNCTIONAL_GROUPS = new CDKFingerprintSet("CDK Functional Groups", new SubstructureFingerprinter(
					StandardSubstructureSets.getFunctionalGroupSMARTS()));
			FINGERPRINTS[0] = FUNCTIONAL_GROUPS;
			FINGERPRINTS[1] = new CDKFingerprintSet("CDK Klekota-Roth Biological Activity",
					new KlekotaRothFingerprinter());
		}
		catch (Exception e)
		{
			Settings.LOGGER.error(e);
		}
	}

	SubstructureFingerprinter fingerprinter;

	private CDKFingerprintSet(String name, SubstructureFingerprinter fingerprinter)
	{
		super(name, SubstructureType.MATCH);
		this.fingerprinter = fingerprinter;
	}

	public static CDKFingerprintSet fromString(String string)
	{
		for (CDKFingerprintSet set : FINGERPRINTS)
		{
			if (set.toString().equals(string))
				return set;
		}
		return null;
	}

	@Override
	public boolean equals(Object o)
	{
		return (o instanceof CDKFingerprintSet) && ((CDKFingerprintSet) o).toString().equals(toString());
	}

	@Override
	public String getDescription()
	{
		return Settings.text("features.struct.cdk.desc", Settings.CDK_STRING, fingerprinter.getSize() + "",
				CDKDescriptor.getAPILink(fingerprinter.getClass()));
	}

	@Override
	public boolean compute(DatasetFile dataset)
	{
		LinkedHashMap<String, DefaultFragmentProperty> smartsToProp = new LinkedHashMap<String, DefaultFragmentProperty>();
		HashMap<DefaultFragmentProperty, String[]> hash = new HashMap<DefaultFragmentProperty, String[]>();

		TaskProvider.debug("Computing CDK fingerprint for compounds");
		for (int m = 0; m < dataset.numCompounds(); m++)
		{
			TaskProvider.verbose("Computing CDK fingerprint for compound " + (m + 1) + "/" + dataset.numCompounds());
			//			TaskProvider.task().verbose("Total number of matched structural fragments (unfiltered): " + ps.size());

			IAtomContainer mol = dataset.getCompounds()[m];
			try
			{
				IBitFingerprint bs = fingerprinter.getBitFingerprint(mol);
				for (int i = 0; i < bs.size(); i++)
				{
					if (bs.get(i))
					{
						String smarts = fingerprinter.getSubstructure(i);
						if (!smartsToProp.containsKey(smarts))
						{
							smartsToProp.put(smarts, new DefaultFragmentProperty(this, smarts, "Structural Fragment",
									smarts, MatchEngine.CDK));
							String values[] = new String[dataset.numCompounds()];
							Arrays.fill(values, "0");
							hash.put(smartsToProp.get(smarts), values);
						}
						DefaultFragmentProperty prop = smartsToProp.get(smarts);
						String values[] = hash.get(prop);
						if (m > values.length - 1)
							throw new IllegalStateException("illegal index: " + m + ", length: " + values.length + ", "
									+ ArrayUtil.toString(values));
						values[m] = "1";
					}
				}
			}
			catch (CDKException e)
			{
				Settings.LOGGER.error(e);
			}
			if (!TaskProvider.isRunning())
				return false;
		}
		List<DefaultFragmentProperty> ps = new ArrayList<DefaultFragmentProperty>();
		for (DefaultFragmentProperty p : smartsToProp.values())
		{
			String values[] = hash.get(p);
			CountedSet<String> cs = CountedSet.fromArray(values);
			if (cs.getCount("1") + cs.getCount("0") != dataset.numCompounds())
				throw new IllegalStateException();
			p.setFrequency(cs.getCount("1"));
			//			Settings.LOGGER.println("freq: " + p.getSmarts() + " " + cs.getCount("1"));
			p.setStringValues(values);
			ps.add(p);
		}

		props.put(dataset, ps);
		updateFragments();
		return true;
	}

	@Override
	public boolean isComputationSlow()
	{
		return fingerprinter.getSize() > 1000;
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
