package data.cdkfingerprints;

import java.util.HashMap;

import dataInterface.AbstractFragmentProperty;

public class CDKFingerprintProperty extends AbstractFragmentProperty
{
	CDKFingerprintSet set;

	private CDKFingerprintProperty(CDKFingerprintSet set, String name, String smarts)
	{
		super(name, set + "_" + name + "_" + smarts, "Structural Fragment", smarts);
		this.set = set;
	}

	private static HashMap<String, CDKFingerprintProperty> instances = new HashMap<String, CDKFingerprintProperty>();

	static CDKFingerprintProperty create(CDKFingerprintSet set, String name, String smarts)
	{
		String key = set + "_" + name + "_" + smarts;
		if (!instances.containsKey(key))
			instances.put(key, new CDKFingerprintProperty(set, name, smarts));
		return instances.get(key);
	}

	@Override
	public CDKFingerprintSet getMoleculePropertySet()
	{
		return set;
	}
}
