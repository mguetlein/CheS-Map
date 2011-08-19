package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import main.Settings;
import util.CharUtil;
import util.ExternalToolUtil;
import util.StringUtil;
import dataInterface.AbstractMoleculeProperty;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculePropertySet;

public class OBFingerprintProperty extends AbstractMoleculeProperty
{

	public enum FingerprintType
	{
		FP2, FP3, FP4, MACCS
	}

	public static final OBFingerPrints[] FINGERPRINTS = new OBFingerPrints[FingerprintType.values().length];
	static
	{
		int count = 0;
		for (FingerprintType t : FingerprintType.values())
			FINGERPRINTS[count++] = new OBFingerPrints(t);
	}

	public static class OBFingerPrints implements MoleculePropertySet
	{
		FingerprintType type;

		public OBFingerPrints(FingerprintType type)
		{
			this.type = type;
		}

		@Override
		public int getSize()
		{
			if (type == FingerprintType.FP2)
				return 1024;
			if (type == FingerprintType.FP3)
				return 64;
			if (type == FingerprintType.FP4)
				return 512;
			if (type == FingerprintType.MACCS)
				return 256;
			throw new IllegalStateException();
		}

		@Override
		public MoleculeProperty get(int index)
		{
			return OBFingerprintProperty.create(type, index);
		}

		public String toString()
		{
			return type.toString();
		}

		public static OBFingerPrints fromString(String string)
		{
			return new OBFingerPrints(FingerprintType.valueOf(string));
		}

		@Override
		public boolean equals(Object o)
		{
			return (o instanceof OBFingerPrints) && ((OBFingerPrints) o).type.equals(type);
		}
	}

	FingerprintType type;
	int index;

	private OBFingerprintProperty(FingerprintType finger, int index)
	{
		this.type = finger;
		this.index = index;

		setTypeAllowed(Type.NUMERIC, false);
		setType(Type.NOMINAL);
		setNominalDomain(new String[] { "0", "1" });
	}

	private static List<OBFingerprintProperty> instances = new ArrayList<OBFingerprintProperty>();

	public static OBFingerprintProperty create(FingerprintType type, int index)
	{
		OBFingerprintProperty p = new OBFingerprintProperty(type, index);
		if (instances.indexOf(p) == -1)
		{
			instances.add(p);
			return p;
		}
		else
		{
			return instances.get(instances.indexOf(p));
		}
	}

	public String toString()
	{
		return type + "_" + index;
	}

	@Override
	public boolean equals(Object o)
	{
		return (o instanceof OBFingerprintProperty) && ((OBFingerprintProperty) o).type.equals(type)
				&& ((OBFingerprintProperty) o).index == index;
	}

	public int numSetValues()
	{
		return new OBFingerPrints(type).getSize();
	}

	public List<String> compute(DatasetFile dataset)
	{
		if (dataset.getSDFPath(false) == null)
			FeatureService.writeSDFFile(dataset);

		try
		{
			String filepath = Settings.destinationFile(dataset.getLocalPath(), dataset.getName() + ".fingerprint.hex");

			String cmd = Settings.CV_BABEL_PATH + " " + dataset.getSDFPath(false) + " -ofpt -xf" + type + " -xho";
			ExternalToolUtil.run("ob-fingerprints", cmd, new File(filepath));

			BufferedReader buffy = new BufferedReader(new FileReader(new File(filepath)));
			String s = null;
			String hex = "";
			List<String> hexFingerprints = new ArrayList<String>();
			int hexSize = new OBFingerPrints(type).getSize() / 4;

			while ((s = buffy.readLine()) != null)
			{
				// babel 2.3.0
				if (!CharUtil.isHexChar(s.charAt(0)))
					continue;
				StringTokenizer tok = new StringTokenizer(s, " ");
				while (tok.hasMoreElements())
				{
					String ss = tok.nextToken();
					//					System.out.println(ss);
					hex += ss;
					if (hex.length() == hexSize)
					{
						//						System.out.println(hexFingerprints.size() + " : " + hex);
						hexFingerprints.add(hex);
						hex = "";
					}
					else if (hex.length() > hexSize)
						throw new Error("to long (" + hex.length() + " > " + hexSize + ") : " + hex);
				}
			}
			if (hex.length() != 0)
				throw new Error("hex-leftover: " + hex);

			List<String> binFingerprints = new ArrayList<String>();
			for (String string : hexFingerprints)
			{
				String bin = "";
				for (int i = 0; i < string.length(); i++)
					bin += StringUtil.concatChar(Integer.toBinaryString(Character.digit(string.charAt(i), 16)), 4, '0',
							false);
				//				System.out.println(binFingerprints.size() + " : " + bin);
				binFingerprints.add(bin);
			}

			return binFingerprints;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
}
