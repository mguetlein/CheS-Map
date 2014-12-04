package org.chesmapper.map.property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.JOptionPane;

import org.chesmapper.map.data.CDKSmartsHandler;
import org.chesmapper.map.data.fragments.MatchEngine;
import org.chesmapper.map.dataInterface.FragmentPropertySet;
import org.chesmapper.map.dataInterface.FragmentProperty.SubstructureType;
import org.chesmapper.map.main.Settings;
import org.mg.javalib.io.JarUtil;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.ListUtil;
import org.mg.javalib.util.StringUtil;
import org.mg.javalib.util.FileUtil.CSVFile;

public class ListedFragments
{
	List<FragmentPropertySet> fragmentListAll = new ArrayList<FragmentPropertySet>();

	private static final String[] included = new String[] { "patterns.csv", "SMARTS_InteLigand.csv", "MACCS.csv",
			"DNA.csv", "Phospholipidosis.csv", "Protein.csv", "ToxTree_BB_CarcMutRules.csv",
			"ToxTree_BiodgeradationRules.csv", "ToxTree_CramerRules.csv", "ToxTree_CramerRulesWithExtensions.csv",
			"ToxTree_EyeIrritationRules.csv", "ToxTree_FuncRules.csv", "ToxTree_Kroes1Tree.csv",
			"ToxTree_MichaelAcceptorRules.csv", "ToxTree_MICRules.csv", "ToxTree_SicretRules.csv",
			"ToxTree_SkinSensitisationPlugin.csv", "ToxTree_SMARTCYPPlugin.csv", "ToxTree_VerhaarScheme2.csv",
			"ToxTree_VerhaarScheme.csv" };

	public static final String SMARTS_LIST_PREFIX = "Smarts list: ";

	private static HashMap<String, String> nameSuffixes = new HashMap<String, String>();
	{
		nameSuffixes.put("patterns", "(OpenBabel FP3)");
		nameSuffixes.put("SMARTS_InteLigand", "(OpenBabel FP4)");
		nameSuffixes.put("MACCS", "(OpenBabel MACCS)");
	}

	private static final ListedFragments instance = new ListedFragments();

	//inits to add listeners 
	public static void init()
	{
		instance.toString();
	}

	private ListedFragments()
	{
		for (String f : included)
			JarUtil.extractFromJAR("structural_fragments/" + f, Settings.getFragmentFileDestination(f), false);
		doReset(null);
	}

	public static void reset(String showWarningForFile)
	{
		instance.doReset(showWarningForFile);
	}

	public void doReset(String showWarningForFile)
	{
		fragmentListAll.clear();

		fragmentListAll.add(MossFragmentSet.INSTANCE);

		for (OBFingerprintSet fp : OBFingerprintSet.FINGERPRINTS)
			fragmentListAll.add(fp);

		for (CDKFingerprintSet fp : CDKFingerprintSet.FINGERPRINTS)
			fragmentListAll.add(fp);

		String files[] = Settings.getFragmentFiles();
		Arrays.sort(files);
		for (int i = included.length - 1; i >= 0; i--)
		{
			int idx = -1;
			for (int j = 0; j < files.length; j++)
				if (files[j].contains(included[i]))
				{
					idx = j;
					break;
				}
			if (idx == -1)
				throw new IllegalArgumentException(included[i] + " not in " + ArrayUtil.toString(files));
			String tmp = files[idx];
			files[idx] = files[i];
			files[i] = tmp;
		}

		for (String filename : files)
		{
			try
			{
				String warnings = "";
				HashMap<MatchEngine, List<ListedFragmentProperty>> a = new HashMap<MatchEngine, List<ListedFragmentProperty>>();
				for (MatchEngine m : MatchEngine.values())
					a.put(m, new ArrayList<ListedFragmentProperty>());

				CSVFile csv = FileUtil.readCSV(filename, ",");
				for (String[] line : csv.content)
				{

					if (line.length == 2 || (line.length == 3 && StringUtil.isInteger(line[2])))
					{
						boolean match = true;
						String name = line[0];
						int inc = 1;
						String finalName = "";
						while (match)
						{
							match = false;
							finalName = inc == 1 ? name : name + "[" + inc + "]";
							for (ListedFragmentProperty sf : a.get(MatchEngine.values()[0]))
							{
								if (sf.getName().equals(finalName))
								{
									match = true;
									inc++;
									break;
								}
							}
						}
						for (MatchEngine m : MatchEngine.values())
						{
							a.get(m).add(
									new ListedFragmentProperty(finalName, "Structural Fragment, matched with " + m,
											line[1], m, line.length == 2 ? 0 : Integer.parseInt(line[2])));
						}
					}
					else
						throw new IllegalStateException("Illegal substructure format in file: '" + filename
								+ "'\nin line '" + a.get(MatchEngine.CDK).size()
								+ "' (should be <name>,\"<smarts>\"[,num-matches]), line:\n" + ArrayUtil.toString(line)
								+ " " + line.length);
					if (FileUtil.getFilename(filename).equals(showWarningForFile))
						if (!CDKSmartsHandler.isSMARTS(line[1]))
							warnings += "Not a valid SMARTS string: '" + line[1] + "'\n";
				}

				if (warnings.length() > 0)
					JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, "Warnings while parsing SMARTS file '"
							+ showWarningForFile + "':\n" + warnings, "Warnings while parsing SMARTS file",
							JOptionPane.WARNING_MESSAGE);

				String fname = FileUtil.getFilename(filename, false);
				String name = SMARTS_LIST_PREFIX + fname;
				if (nameSuffixes.containsKey(fname))
					name += " " + nameSuffixes.get(fname);
				String desc = "Num smarts strings: " + a.get(MatchEngine.CDK).size();
				desc += "\nLocation: " + filename;
				String comments = ListUtil.toString(csv.comments, "\n");
				comments = comments.substring(2, comments.length() - 2);
				desc += "\nComments:\n" + comments;
				fragmentListAll.add(new ListedFragmentSet(name, desc, a));
			}
			catch (Exception e)
			{
				Settings.LOGGER.error(e);

				if (FileUtil.getFilename(filename).equals(showWarningForFile))
					JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, "Could not parse SMARTS file '"
							+ showWarningForFile + "':\n" + e.getMessage(), "Could not parse SMARTS file",
							JOptionPane.ERROR_MESSAGE);
			}
		}

		fragmentListAll.add(FminerPropertySet.INSTANCE);
	}

	public static int getNumSets()
	{
		return instance.fragmentListAll.size();
	}

	public static FragmentPropertySet[] getSets(SubstructureType type)
	{
		List<FragmentPropertySet> l = new ArrayList<FragmentPropertySet>();
		for (FragmentPropertySet f : instance.fragmentListAll)
			if (f.getSubstructureType() == type)
				l.add(f);
		return ListUtil.toArray(FragmentPropertySet.class, l);
	}

	public static FragmentPropertySet findFromString(String string)
	{
		for (FragmentPropertySet a : instance.fragmentListAll)
		{
			if (a.toString().equals(string))
				return a;
		}
		return null;
	}

}
