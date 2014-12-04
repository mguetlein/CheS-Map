package org.chesmapper.map.property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.AbstractPropertySet;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.DefaultNominalProperty;
import org.chesmapper.map.dataInterface.FragmentProperty.SubstructureType;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.mg.javalib.gui.binloc.Binary;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;

public class CDKHashedFingerprintSet extends AbstractPropertySet
{
	public static CDKHashedFingerprintSet[] FINGERPRINTS = null;
	static
	{
		try
		{
			FINGERPRINTS = new CDKHashedFingerprintSet[] { new CDKHashedFingerprintSet("ecfp6",
					new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP6)) };
		}
		catch (Exception e)
		{
			Settings.LOGGER.error(e);
		}
	}

	String name;
	IFingerprinter fingerprinter;
	HashMap<DatasetFile, List<DefaultNominalProperty>> props = new HashMap<DatasetFile, List<DefaultNominalProperty>>();

	private CDKHashedFingerprintSet(String name, IFingerprinter fingerprinter)
	{
		this.name = name;
		this.fingerprinter = fingerprinter;
	}

	public static CDKHashedFingerprintSet fromString(String string)
	{
		for (CDKHashedFingerprintSet set : FINGERPRINTS)
			if (set.serialize().equals(string))
				return set;
		return null;
	}

	@Override
	public boolean equals(Object o)
	{
		return (o instanceof CDKHashedFingerprintSet) && ((CDKHashedFingerprintSet) o).toString().equals(toString());
	}

	public String toString()
	{
		return Settings.text("features.hashed." + name);
	}

	@Override
	public String getDescription()
	{
		return Settings.text("features.hashed." + name + ".desc", Settings.CDK_STRING, fingerprinter.getSize() + "",
				CDKDescriptor.getAPILink(fingerprinter.getClass()));
	}

	@Override
	public boolean compute(DatasetFile dataset)
	{
		List<IBitFingerprint> fingerprints = new ArrayList<IBitFingerprint>();
		TaskProvider.debug("Computing CDK fingerprint for compounds");
		for (int c = 0; c < dataset.numCompounds(); c++)
		{
			TaskProvider.verbose("Computing CDK fingerprint for compound " + (c + 1) + "/" + dataset.numCompounds());
			//			TaskProvider.task().verbose("Total number of matched structural fragments (unfiltered): " + ps.size());
			IAtomContainer mol = dataset.getCompounds()[c];
			try
			{
				fingerprints.add(fingerprinter.getBitFingerprint(mol));
			}
			catch (CDKException e)
			{
				Settings.LOGGER.error(e);
			}
			if (!TaskProvider.isRunning())
				return false;
		}

		props.put(dataset, new ArrayList<DefaultNominalProperty>(fingerprinter.getSize()));
		for (int b = 0; b < fingerprinter.getSize(); b++)
		{
			DefaultNominalProperty val = new DefaultNominalProperty(this, "Bit " + b + " of " + name, "yet to come");
			String vals[] = new String[dataset.numCompounds()];
			for (int c = 0; c < dataset.numCompounds(); c++)
				vals[c] = fingerprints.get(c).get(b) ? "1" : "0";
			val.setStringValues(vals);
			props.get(dataset).add(val);
		}
		return true;
	}

	@Override
	public boolean isComputed(DatasetFile dataset)
	{
		return props.containsKey(dataset);
	}

	@Override
	public boolean isCached(DatasetFile dataset)
	{
		return false;
	}

	@Override
	public boolean isSizeDynamic()
	{
		return false;
	}

	@Override
	public int getSize(DatasetFile d)
	{
		return fingerprinter.getSize();
	}

	@Override
	public CompoundProperty get(DatasetFile d, int index)
	{
		return props.get(d).get(index);
	}

	@Override
	public void clearComputedProperties(DatasetFile d)
	{
		props.remove(d);
	}

	@Override
	public String serialize()
	{
		return name;
	}

	@Override
	public Binary getBinary()
	{
		return null;
	}

	@Override
	public boolean isSelectedForMapping()
	{
		return true;
	}

	@Override
	public String getNameIncludingParams()
	{
		return name;
	}

	@Override
	public boolean isComputationSlow()
	{
		return false;
	}

	@Override
	public boolean isSensitiveTo3D()
	{
		return false;
	}

	@Override
	public SubstructureType getSubstructureType()
	{
		return null;
	}

	@Override
	public Type getType()
	{
		return Type.NOMINAL;
	}

	@Override
	public boolean isTypeAllowed(Type type)
	{
		return type == Type.NOMINAL;
	}

}