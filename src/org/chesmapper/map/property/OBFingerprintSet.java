package org.chesmapper.map.property;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.fragments.FragmentProperties;
import org.chesmapper.map.data.fragments.MatchEngine;
import org.chesmapper.map.dataInterface.DefaultFragmentProperty;
import org.chesmapper.map.dataInterface.FragmentPropertySet;
import org.chesmapper.map.dataInterface.FragmentProperty.SubstructureType;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.chesmapper.map.util.ExternalToolUtil;
import org.mg.javalib.gui.binloc.Binary;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.ListUtil;

public class OBFingerprintSet extends FragmentPropertySet
{
	static final OBFingerprintSet[] FINGERPRINTS;

	static
	{
		FINGERPRINTS = new OBFingerprintSet[OBFingerprintType.values().length];
		int count = 0;
		for (OBFingerprintType t : OBFingerprintType.values())
			FINGERPRINTS[count++] = new OBFingerprintSet(t);
	}

	OBFingerprintType type;
	String description;
	final boolean hidden;

	static OBFingerprintSet getOBFingerprintSet(OBFingerprintType type)
	{
		for (OBFingerprintSet s : FINGERPRINTS)
			if (s.getOBType() == type)
				return s;
		throw new IllegalArgumentException();
	}

	private OBFingerprintSet(OBFingerprintType type)
	{
		super();
		this.type = type;

		switch (type)
		{
			case FP2:
				name = Settings.text("features.struct.fp2");
				description = Settings.text("features.struct.fp2.desc");
				substructureType = SubstructureType.MINE;
				hidden = false;
				break;
			case FP3:
				name = Settings.text("features.struct.fp3");
				description = Settings.text("features.struct.fp3.desc");
				substructureType = SubstructureType.MATCH;
				hidden = true;
				break;
			case FP4:
				name = Settings.text("features.struct.fp4");
				description = Settings.text("features.struct.fp4.desc");
				substructureType = SubstructureType.MATCH;
				hidden = true;
				break;
			case MACCS:
				name = Settings.text("features.struct.maccs");
				description = Settings.text("features.struct.maccs.desc");
				substructureType = SubstructureType.MATCH;
				hidden = true;
				break;
			default:
				throw new Error("Unknown type");
		}
	}

	@Override
	public boolean isHiddenFromGUI()
	{
		return hidden;
	}

	@Override
	public Binary getBinary()
	{
		return BinHandler.BABEL_BINARY;
	}

	public static OBFingerprintSet fromString(String string)
	{
		return new OBFingerprintSet(OBFingerprintType.valueOf(string));
	}

	@Override
	public boolean equals(Object o)
	{
		return (o instanceof OBFingerprintSet) && ((OBFingerprintSet) o).type.equals(type);
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

	public OBFingerprintType getOBType()
	{
		return type;
	}

	static class FPFragment
	{
		private String smarts;
		private String name;
		private String line;

		private static HashMap<String, FPFragment> uniqueFragments = new HashMap<String, FPFragment>();

		public static FPFragment[] parse(OBFingerprintType type, String line) throws IOException
		{
			FPFragment[] newF;
			switch (type)
			{
				case FP2:
					newF = parseFP2Fragment(line);
					break;
				case FP3:
					newF = parseFP3Fragment(line);
					break;
				case FP4:
					newF = parseFP4Fragment(line);
					break;
				case MACCS:
					newF = parseMACCSFragment(line);
					break;
				default:
					throw new Error("type not handled: " + type);
			}

			for (int i = 0; i < newF.length; i++)
			{
				String key = newF[i].name + " " + newF[i].smarts;
				if (!uniqueFragments.containsKey(key))
					uniqueFragments.put(key, newF[i]);
				newF[i] = uniqueFragments.get(key);
			}
			return newF;
		}

		private static HashMap<String, FPFragment> fp3Fragments = new HashMap<String, FPFragment>();

		private static FPFragment[] parseFP3Fragment(String line) throws IOException
		{
			BufferedReader buffy = null;
			try
			{
				if (fp3Fragments.size() == 0)
				{
					buffy = new BufferedReader(new FileReader(new File(BinHandler.getOBFileOrig("patterns.txt"))));
					String s = "";
					while ((s = buffy.readLine()) != null)
					{
						if (!s.startsWith("#") && s.trim().length() > 0)
						{
							String ss[] = s.split("\\s");
							if (ss.length < 3)
								throw new Error("WTF: " + ArrayUtil.toString(ss));
							FPFragment f = new FPFragment();
							f.line = s;
							f.smarts = ss[0];
							String name = "";
							for (int i = 2; i < ss.length; i++)
								name += " " + ss[i];
							f.name = name;
							fp3Fragments.put(ss[1], f);
						}
					}
				}
				List<FPFragment> l = new ArrayList<FPFragment>();
				for (String s : line.split("\\s"))
				{
					if (!fp3Fragments.containsKey(s))
						throw new Error("key not found: " + s + ", keys: " + fp3Fragments.keySet());
					l.add(fp3Fragments.get(s));
				}
				FPFragment f[] = new FPFragment[l.size()];
				return l.toArray(f);
			}
			finally
			{
				if (buffy != null)
					buffy.close();
			}
		}

		private static HashMap<String, FPFragment> fp4Fragments = new HashMap<String, FPFragment>();

		private static FPFragment[] parseFP4Fragment(String line) throws IOException
		{
			BufferedReader buffy = null;
			try
			{
				if (fp4Fragments.size() == 0)
				{
					buffy = new BufferedReader(new FileReader(BinHandler.getOBFileOrig("SMARTS_InteLigand.txt")));
					String s = "";
					while ((s = buffy.readLine()) != null)
					{
						if (!s.startsWith("#") && s.trim().length() > 0)
						{
							int i = s.indexOf(':');
							if (i == -1)
								throw new Error("WTF: " + line);
							FPFragment f = new FPFragment();
							f.line = s;
							f.smarts = s.substring(i + 1).trim();
							String name = s.substring(0, i).trim();
							f.name = name;
							fp4Fragments.put(name, f);
						}
					}
				}
				List<FPFragment> l = new ArrayList<FPFragment>();
				for (String s : line.split("\\s"))
				{
					if (!fp4Fragments.containsKey(s))
						throw new Error("key not found: " + s + ", keys: " + fp4Fragments.keySet());
					l.add(fp4Fragments.get(s));
				}
				FPFragment f[] = new FPFragment[l.size()];
				return l.toArray(f);
			}
			finally
			{
				if (buffy != null)
					buffy.close();
			}
		}

		private static HashMap<String, FPFragment> maccsFragments = new HashMap<String, FPFragment>();

		private static FPFragment[] parseMACCSFragment(String line) throws IOException
		{
			BufferedReader buffy = null;
			try
			{
				// Settings.LOGGER.warn("frags: " + line);
				if (maccsFragments.size() == 0)
				{
					// 155:('*!@[CH2]!@*',0), # A!CH2!A
					buffy = new BufferedReader(new FileReader(new File(BinHandler.getOBFileOrig("MACCS.txt"))));
					String s = "";
					while ((s = buffy.readLine()) != null)
					{
						if (!s.trim().startsWith("#") && s.trim().length() > 0)
						{
							// Settings.LOGGER.warn(s);
							Pattern pattern = Pattern.compile("^\\s*([0-9]++\\:)\\(\\'(.*)\\',[0-9]\\),.*#(.*)$");
							Matcher matcher = pattern.matcher(s);
							boolean matchFound = matcher.find();
							if (!matchFound)
								throw new Error("WTF: " + s);
							// for (int i = 0; i <= matcher.groupCount(); i++)
							// Settings.LOGGER.warn(i + " " + matcher.group(i));
							FPFragment f = new FPFragment();
							f.line = s;
							f.smarts = matcher.group(2);
							String name = matcher.group(3);
							int i = name.indexOf("FIX:");
							if (i == -1)
								i = name.indexOf("*NOTE*");
							if (i != -1)
								name = name.substring(0, i);
							String space = name.startsWith(" ") ? "" : " ";
							f.name = name.trim();
							String key = (matcher.group(1) + space + name).trim();
							maccsFragments.put(key, f);
						}
					}
				}
				List<FPFragment> l = new ArrayList<FPFragment>();
				boolean minFreq = false;
				for (String s : line.split("\\t"))
				{
					s = s.trim();
					if (minFreq && s.matches("^\\*[2-4].*"))
						s = s.substring(2).trim();
					if (s.matches(".*\\*[2-4]$"))
						s = s.substring(0, s.length() - 2).trim();
					// Settings.LOGGER.warn("key: " + s);
					if (!maccsFragments.containsKey(s))
						throw new Error("key not found: '" + s + "', keys: "
								+ ListUtil.toString(new ArrayList<String>(maccsFragments.keySet()), "\n"));
					minFreq = s.matches(".*>(\\s)*[1-3].*");
					l.add(maccsFragments.get(s));

				}
				FPFragment f[] = new FPFragment[l.size()];
				return l.toArray(f);
			}
			finally
			{
				if (buffy != null)
					buffy.close();
			}
		}

		private static String BONDS[] = { null, "-", "=", "#", null, ":" };

		// private static HashMap<Integer, List<String>> hashKeyNum = new HashMap<Integer, List<String>>();
		//
		// private static String getUniqueHashKey(Integer hashKey, String smarts)
		// {
		// if (!hashKeyNum.containsKey(hashKey))
		// hashKeyNum.put(hashKey, new ArrayList<String>());
		// if (hashKeyNum.get(hashKey).indexOf(smarts) == -1)
		// hashKeyNum.get(hashKey).add(smarts);
		// return hashKey + "-" + hashKeyNum.get(hashKey).indexOf(smarts);
		// }

		/**
		 * 0 11 1 7 5 6 5 6 <474>
		 * 
		 * @param line
		 */
		private static FPFragment[] parseFP2Fragment(String line)
		{
			FPFragment f = new FPFragment();

			f.line = line;
			String ringBond = "";
			// Integer hashKey = null;

			f.smarts = "";
			String s[] = line.split(" ");
			for (int i = 0; i < s.length; i++)
			{
				if (i == 0)
				{
					ringBond = BONDS[Integer.parseInt(s[0])];
				}
				else if (i == s.length - 1)
				{
					Integer.parseInt(s[i].substring(1, s[i].length() - 1));
					// hashKey = Integer.parseInt(s[i].substring(1, s[i].length() - 1));
				}
				else if (i % 2 == 1) // atom
				{
					f.smarts += "[#" + s[i] + "]";
					if (ringBond != null && i == 1)
						f.smarts += "1";
					// smarts += PeriodicTable.getSymbol(Integer.parseInt(s[i]));
				}
				else if (i % 2 == 0) // atom
					f.smarts += BONDS[Integer.parseInt(s[i])];
			}
			if (ringBond != null)
			{
				f.smarts += ringBond;
				f.smarts += "1";
			}
			f.name = f.smarts; // "Linfrag " + getUniqueHashKey(hashKey, f.smarts);
			return new FPFragment[] { f };
		}

		public String toString()
		{
			return line;
		}

	}

	@Override
	public boolean compute(DatasetFile dataset)
	{
		String version = BinHandler.getOpenBabelVersion();
		int index = version.indexOf('.');
		int major = Integer.parseInt(version.substring(0, index));
		int nIndex = version.indexOf('.', index + 1);
		int minor = Integer.parseInt(version.substring(index + 1, nIndex));
		if (major < 2 || (major == 2 && minor < 3))
		{
			TaskProvider.warning("OpenBabel fingerprints require OpenBabel version >= 2.3, your version '" + version
					+ "'", "");
			return false;
		}

		File tmp = null;
		BufferedReader buffy = null;
		try
		{
			Settings.LOGGER.info("computing structural fragment " + FragmentProperties.getMatchEngine() + " "
					+ FragmentProperties.getMinFrequency() + " " + FragmentProperties.isSkipOmniFragments());

			List<String[]> featureValues = new ArrayList<String[]>();

			// if (obProp.getOBType() != OBFingerprintProperty.FingerprintType.FP2)
			// {
			// List<String> fingerprintsForCompounds = obProp.compute(dataset);
			// int numMols = fileToCompounds.get(dataset).length;
			// if (fingerprintsForCompounds.size() != numMols)
			// throw new IllegalStateException("babel returned fingerprints for "
			// + fingerprintsForCompounds.size() + " compounds, but dataset contains " + numMols
			// + " compounds.");
			// int numFingerprints = obProp.getSize(dataset);
			// if (fingerprintsForCompounds.get(0).length() != numFingerprints)
			// throw new IllegalStateException("fingerprint length not correct");
			//
			// for (int j = 0; j < numFingerprints; j++)
			// {
			// String[] featureValue = new String[numMols];
			// for (int i = 0; i < numMols; i++)
			// featureValue[i] = fingerprintsForCompounds.get(i).charAt(j) + "";
			// featureValues.add(featureValue);
			// }
			// }
			// else
			// {

			LinkedHashMap<FPFragment, List<Integer>> occurences = new LinkedHashMap<FPFragment, List<Integer>>();

			tmp = File.createTempFile(dataset.getShortName(), "OBfingerprint");
			String cmd[] = { BinHandler.BABEL_BINARY.getLocation(), "-isdf", dataset.getSDF(), "-ofpt", "-xf",
					type.toString(), "-xs" };
			TaskProvider.debug("Running babel: " + ArrayUtil.toString(cmd, " ", "", ""));
			ExternalToolUtil.run("ob-fingerprints", cmd, tmp);

			TaskProvider.debug("Parsing fingerprints");
			buffy = new BufferedReader(new FileReader(tmp));
			String s = null;

			int count = -1;
			while ((s = buffy.readLine()) != null)
			{
				// Settings.LOGGER.warn();
				// Settings.LOGGER.warn(count);
				// Settings.LOGGER.warn(s);
				if (s.startsWith(">"))
				{
					count++;
					s = ""; //s.replaceAll("^>[^\\s]*", "").trim();
				}
				// Settings.LOGGER.warn(s);
				if (s.length() > 0)
				{
					FPFragment frag[] = FPFragment.parse(type, s);
					for (FPFragment fpFragment : frag)
					{
						if (!occurences.containsKey(fpFragment))
							occurences.put(fpFragment, new ArrayList<Integer>());
						occurences.get(fpFragment).add(count);
					}
				}
			}

			// for (FP2Fragment frag : occurences.keySet())
			// Settings.LOGGER.warn(frag + " " + ListUtil.toString(occurences.get(frag)));

			if (dataset.numCompounds() - 1 != count)
				throw new Error("num compounds not correct " + dataset.numCompounds() + " " + count);

			List<DefaultFragmentProperty> ps = new ArrayList<DefaultFragmentProperty>();
			for (FPFragment fp : occurences.keySet())
			{
				DefaultFragmentProperty p = new DefaultFragmentProperty(this, fp.name.trim(), "Structural Fragment",
						fp.smarts, MatchEngine.OpenBabel);
				p.setFrequency(occurences.get(fp).size());

				String[] featureValue = new String[dataset.numCompounds()];
				for (int i = 0; i < dataset.numCompounds(); i++)
					featureValue[i] = occurences.get(fp).contains(new Integer(i)) ? "1" : "0";
				featureValues.add(featureValue);
				p.setStringValues(featureValue);

				ps.add(p);
			}

			props.put(dataset, ps);
			updateFragments();
			return true;
		}
		catch (Throwable e)
		{
			Settings.LOGGER.error(e);
			TaskProvider.warning("Could not compute OpenBabel fingerprint " + this, e);
			// for (int j = 0; j < getSize(dataset); j++)
			// get(dataset, j).setStringValues(dataset, new String[dataset.numCompounds()]);
			return false;
		}
		finally
		{
			tmp.delete();
			if (buffy != null)
				try
				{
					buffy.close();
				}
				catch (IOException e)
				{
					Settings.LOGGER.error(e);
				}
		}
	}

	@Override
	public boolean isSelectedForMapping()
	{
		return true;
	}

	@Override
	public boolean isSizeDynamicHigh(DatasetFile dataset)
	{
		return type == OBFingerprintType.FP2 && FragmentProperties.getMinFrequency() <= 2;
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
