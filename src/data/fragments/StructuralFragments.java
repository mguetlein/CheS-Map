package data.fragments;

import io.JarUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.JOptionPane;

import main.Settings;
import util.ArrayUtil;
import util.FileUtil;
import util.FileUtil.CSVFile;
import util.ListUtil;
import util.StringUtil;
import data.CDKSmartsHandler;
import data.cdkfingerprints.CDKFingerprintSet;
import data.obfingerprints.OBFingerprintSet;
import dataInterface.AbstractCompoundProperty;
import dataInterface.FragmentPropertySet;

public class StructuralFragments
{
	List<FragmentPropertySet> fragmentList = new ArrayList<FragmentPropertySet>();

	private static final String[] included = new String[] { "patterns.csv", "SMARTS_InteLigand.csv", "MACCS.csv",
			"DNA.csv", "Phospholipidosis.csv", "Protein.csv", "ToxTree_BB_CarcMutRules.csv",
			"ToxTree_BiodgeradationRules.csv", "ToxTree_CramerRules.csv", "ToxTree_CramerRulesWithExtensions.csv",
			"ToxTree_EyeIrritationRules.csv", "ToxTree_FuncRules.csv", "ToxTree_Kroes1Tree.csv",
			"ToxTree_MichaelAcceptorRules.csv", "ToxTree_MICRules.csv", "ToxTree_SicretRules.csv",
			"ToxTree_SkinSensitisationPlugin.csv", "ToxTree_SMARTCYPPlugin.csv", "ToxTree_VerhaarScheme2.csv",
			"ToxTree_VerhaarScheme.csv" };

	private static HashMap<String, String> nameSuffixes = new HashMap<String, String>();
	{
		nameSuffixes.put("patterns", "(OpenBabel FP3)");
		nameSuffixes.put("SMARTS_InteLigand", "(OpenBabel FP4)");
		nameSuffixes.put("MACCS", "(OpenBabel MACCS)");
	}

	public static StructuralFragments instance = new StructuralFragments();

	private StructuralFragments()
	{
		for (String f : included)
			JarUtil.extractFromJAR("structural_fragments/" + f, Settings.getFragmentFileDestination(f), false);
		reset(null);
	}

	public void reset(String showWarningForFile)
	{
		fragmentList.clear();
		AbstractCompoundProperty.clearPropertyOfType(StructuralFragment.class);

		for (OBFingerprintSet fp : OBFingerprintSet.FINGERPRINTS)
			fragmentList.add(fp);

		for (CDKFingerprintSet fp : CDKFingerprintSet.FINGERPRINTS)
			fragmentList.add(fp);

		//		fragmentList.add(new FminerPropertySet());

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
				HashMap<MatchEngine, List<StructuralFragment>> a = new HashMap<MatchEngine, List<StructuralFragment>>();
				for (MatchEngine m : MatchEngine.values())
					a.put(m, new ArrayList<StructuralFragment>());

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
							for (StructuralFragment sf : a.get(MatchEngine.values()[0]))
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
									new StructuralFragment(finalName, m, FileUtil.getFilename(filename, false),
											line[1], line.length == 2 ? 0 : Integer.parseInt(line[2])));
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
				String name = "Smarts list: " + fname;
				if (nameSuffixes.containsKey(fname))
					name += " " + nameSuffixes.get(fname);
				String desc = "Num smarts strings: " + a.get(MatchEngine.CDK).size();
				desc += "\nLocation: " + filename;
				String comments = ListUtil.toString(csv.comments, "\n");
				comments = comments.substring(2, comments.length() - 2);
				desc += "\nComments:\n" + comments;
				fragmentList.add(new StructuralFragmentSet(name, desc, a));
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
	}

	public int getNumSets()
	{
		return fragmentList.size();
	}

	public FragmentPropertySet[] getSets()
	{
		FragmentPropertySet a[] = new FragmentPropertySet[fragmentList.size()];
		return fragmentList.toArray(a);
	}

	public FragmentPropertySet getSet(int i)
	{
		return fragmentList.get(i);
	}

	public FragmentPropertySet findFromString(String string)
	{
		for (FragmentPropertySet a : fragmentList)
		{
			if (a.toString().equals(string))
				return a;
		}
		return null;
	}
}
