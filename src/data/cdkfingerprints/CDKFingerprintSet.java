package data.cdkfingerprints;

import gui.binloc.Binary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

import main.Settings;
import main.TaskProvider;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.KlekotaRothFingerprinter;
import org.openscience.cdk.fingerprint.StandardSubstructureSets;
import org.openscience.cdk.fingerprint.SubstructureFingerprinter;
import org.openscience.cdk.interfaces.IMolecule;

import util.ArrayUtil;
import util.CountedSet;
import data.DatasetFile;
import data.cdk.CDKDescriptor;
import data.fragments.StructuralFragmentProperties;
import dataInterface.FragmentPropertySet;
import dataInterface.CompoundProperty.Type;

public class CDKFingerprintSet extends FragmentPropertySet
{

	public static final CDKFingerprintSet[] FINGERPRINTS = new CDKFingerprintSet[2];
	static
	{
		try
		{
			FINGERPRINTS[0] = new CDKFingerprintSet("CDK Functional Groups", new SubstructureFingerprinter(
					StandardSubstructureSets.getFunctionalGroupSMARTS()));
			FINGERPRINTS[1] = new CDKFingerprintSet("CDK Klekota-Roth Biological Activity",
					new KlekotaRothFingerprinter());

		}
		catch (Exception e)
		{
			Settings.LOGGER.error(e);
		}
	}

	SubstructureFingerprinter fingerprinter;
	String name;

	public CDKFingerprintSet(String name, SubstructureFingerprinter fingerprinter)
	{
		this.fingerprinter = fingerprinter;
		this.name = name;
	}

	private HashMap<DatasetFile, List<CDKFingerprintProperty>> props = new HashMap<DatasetFile, List<CDKFingerprintProperty>>();
	private HashMap<DatasetFile, List<CDKFingerprintProperty>> filteredProps = new HashMap<DatasetFile, List<CDKFingerprintProperty>>();

	@Override
	public int getSize(DatasetFile d)
	{
		if (filteredProps.get(d) == null)
			throw new Error("mine fragments first, number is not fixed");
		return filteredProps.get(d).size();
	}

	@Override
	public CDKFingerprintProperty get(DatasetFile d, int index)
	{
		if (filteredProps.get(d) == null)
			throw new Error("mine fragments first, number is not fixed");
		return filteredProps.get(d).get(index);
	}

	@Override
	public boolean isSizeDynamic()
	{
		return true;
	}

	@Override
	public boolean isComputed(DatasetFile dataset)
	{
		return filteredProps.get(dataset) != null;
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
	protected void updateFragments()
	{
		for (DatasetFile d : props.keySet())
		{
			List<CDKFingerprintProperty> filteredList = new ArrayList<CDKFingerprintProperty>();
			for (CDKFingerprintProperty p : props.get(d))
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

	public String toString()
	{
		return name;
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
	public Type getType()
	{
		return Type.NOMINAL;
	}

	@Override
	public boolean compute(DatasetFile dataset)
	{
		List<CDKFingerprintProperty> ps = new ArrayList<CDKFingerprintProperty>();
		HashMap<CDKFingerprintProperty, String[]> hash = new HashMap<CDKFingerprintProperty, String[]>();
		for (int m = 0; m < dataset.numCompounds(); m++)
		{
			TaskProvider.verbose("Computing CDK fingerprint for compound " + (m + 1) + "/" + dataset.numCompounds());
			//			TaskProvider.task().verbose("Total number of matched structural fragments (unfiltered): " + ps.size());

			IMolecule mol = dataset.getCompounds()[m];
			try
			{
				BitSet bs = fingerprinter.getFingerprint(mol);
				for (int i = 0; i < bs.size(); i++)
				{
					if (bs.get(i))
					{
						String smarts = fingerprinter.getSubstructure(i);
						CDKFingerprintProperty prop = CDKFingerprintProperty.create(this, smarts, smarts);
						if (ps.indexOf(prop) == -1)
							ps.add(prop);
						String values[] = hash.get(prop);
						if (values == null)
						{
							values = new String[dataset.numCompounds()];
							Arrays.fill(values, "0");
							hash.put(prop, values);
						}
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
		for (CDKFingerprintProperty p : ps)
		{
			String values[] = hash.get(p);
			CountedSet<String> cs = CountedSet.fromArray(values);
			if (cs.getCount("1") + cs.getCount("0") != dataset.numCompounds())
				throw new IllegalStateException();
			p.setFrequency(dataset, cs.getCount("1"));
			//			Settings.LOGGER.println("freq: " + p.getSmarts() + " " + cs.getCount("1"));
			p.setStringValues(dataset, values);
		}

		props.put(dataset, ps);
		updateFragments();
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
		return fingerprinter.getSize() > 1000;
	}
}
