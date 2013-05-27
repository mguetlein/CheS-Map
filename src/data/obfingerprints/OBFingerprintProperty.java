package data.obfingerprints;

import java.util.HashMap;

import data.fragments.MatchEngine;
import dataInterface.AbstractFragmentProperty;

public class OBFingerprintProperty extends AbstractFragmentProperty
{
	FingerprintType type;

	private OBFingerprintProperty(FingerprintType obType, String name, String smarts)
	{
		super(name, obType + "_" + name + "_" + smarts, "Structural Fragment", smarts, MatchEngine.OpenBabel);
		this.type = obType;
	}

	private static HashMap<String, OBFingerprintProperty> instances = new HashMap<String, OBFingerprintProperty>();

	static OBFingerprintProperty create(FingerprintType type, String name, String smarts)
	{
		String key = type + "_" + name + "_" + smarts;
		if (!instances.containsKey(key))
			instances.put(key, new OBFingerprintProperty(type, name, smarts));
		return instances.get(key);
	}

	public FingerprintType getOBType()
	{
		return type;
	}

	@Override
	public OBFingerprintSet getCompoundPropertySet()
	{
		return new OBFingerprintSet(type);
	}

	//	@Override
	//	public boolean equals(Object o)
	//	{
	//		return (o instanceof OBFingerprintProperty) && ((OBFingerprintProperty) o).name.equals(name)
	//				&& ((OBFingerprintProperty) o).type.equals(type);
	//	}

}
