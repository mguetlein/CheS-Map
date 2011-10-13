package data;

import gui.binloc.Binary;
import io.JarUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JOptionPane;

import main.Settings;
import util.ArrayUtil;
import util.FileUtil;
import util.FileUtil.CSVFile;
import util.ListUtil;
import data.obfingerprints.OBFingerprintSet;
import dataInterface.AbstractFragmentProperty;
import dataInterface.AbstractMoleculeProperty;
import dataInterface.FragmentPropertySet;
import dataInterface.MoleculeProperty.Type;
import dataInterface.MoleculePropertySet;

public class StructuralFragments
{
	public static class FragmentSet extends FragmentPropertySet
	{
		String description;
		String name;

		HashMap<MatchEngine, List<Fragment>> fragments;
		HashMap<MatchEngine, HashMap<DatasetFile, List<Fragment>>> computedFragments = new HashMap<MatchEngine, HashMap<DatasetFile, List<Fragment>>>();

		public FragmentSet(String name, String description, HashMap<MatchEngine, List<Fragment>> fragments)
		{
			this.fragments = fragments;
			this.description = description;
			this.name = name;

			for (MatchEngine m : fragments.keySet())
			{
				for (Fragment fragment : fragments.get(m))
					fragment.set = this;

				computedFragments.put(m, new HashMap<DatasetFile, List<Fragment>>());
			}
		}

		public String toString()
		{
			return name;
		}

		@Override
		public int getSize(DatasetFile d)
		{
			if (computedFragments.get(matchEngine).get(d) == null)
				throw new Error("mine fp2 fragments first, numer of fp2 fragments is not fixed");
			return computedFragments.get(matchEngine).get(d).size();
		}

		@Override
		public Fragment get(DatasetFile d, int index)
		{
			if (computedFragments.get(matchEngine).get(d) == null)
				throw new Error("mine fp2 fragments first, numer of fp2 fragments is not fixed");
			return computedFragments.get(matchEngine).get(d).get(index);
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

		@Override
		public Binary getBinary()
		{
			if (matchEngine == MatchEngine.OpenBabel)
				return Settings.BABEL_BINARY;
			else
				return null;
		}

		@Override
		protected void updateFragments()
		{
			for (MatchEngine m : computedFragments.keySet())
			{
				for (DatasetFile d : computedFragments.get(m).keySet())
				{
					computedFragments.get(m).get(d).clear();
					for (Fragment a : fragments.get(m))
						addFragment(a, d, m);
				}
			}
		}

		private void addFragment(Fragment a, DatasetFile d, MatchEngine m)
		{
			boolean skip = skipOmnipresent && a.getFrequency(d) == d.numCompounds();
			boolean frequent = a.getFrequency(d) >= minFrequency;
			if (!skip && frequent)
				computedFragments.get(m).get(d).add(a);
		}

		@Override
		public boolean isSizeDynamic()
		{
			return true;
		}

		@Override
		public boolean isComputed(DatasetFile dataset)
		{
			return computedFragments.get(matchEngine).get(dataset) != null;
		}

		@Override
		public boolean compute(DatasetFile dataset)
		{
			MatchEngine matchEngine = this.matchEngine;

			List<String> smarts = new ArrayList<String>();
			for (Fragment fragment : fragments.get(matchEngine))
				smarts.add(fragment.getSmarts());

			List<boolean[]> matches;
			if (matchEngine == MatchEngine.CDK)
				matches = new CDKSmartsHandler().match(smarts, dataset);
			else if (matchEngine == MatchEngine.OpenBabel)
				matches = new OpenBabelSmartsHandler().match(smarts, dataset);
			else
				throw new Error("illegal mactch engine");

			int count = 0;
			for (boolean[] match : matches)
			{
				String m[] = new String[match.length];
				int f = 0;
				for (int i = 0; i < m.length; i++)
				{
					if (match[i])
					{
						m[i] = "1";
						f++;
					}
					else
						m[i] = "0";
				}

				Fragment fragment = fragments.get(matchEngine).get(count);
				fragment.setNominalDomain(new String[] { "0", "1" });
				fragment.setFrequency(dataset, f);
				fragment.setStringValues(dataset, m);
				if (!computedFragments.get(matchEngine).containsKey(dataset))
					computedFragments.get(matchEngine).put(dataset, new ArrayList<StructuralFragments.Fragment>());
				addFragment(fragment, dataset, matchEngine);
				count++;
			}
			return true;
		}

		@Override
		public boolean isUsedForMapping()
		{
			return true;
		}
	}

	public static class Fragment extends AbstractFragmentProperty
	{
		private FragmentSet set;

		public Fragment(String name, MatchEngine matchEngine, String smarts)
		{
			super(name, name + "_" + matchEngine, "Structural Fragment, matched with " + matchEngine, smarts);
		}

		@Override
		public MoleculePropertySet getMoleculePropertySet()
		{
			return set;
		}
	}

	public enum MatchEngine
	{
		CDK, OpenBabel
	}

	List<FragmentPropertySet> fragmentList = new ArrayList<FragmentPropertySet>();

	private static final String[] included = new String[] { "DNA.csv", "Phospholipidosis.csv", "Protein.csv" };
	public static StructuralFragments instance = new StructuralFragments();

	private StructuralFragments()
	{
		for (String f : included)
			JarUtil.extractFromJAR("structural_fragments/" + f, Settings.getFragmentFileDestination(f), false);
		reset(null);
	}

	public void setMinFrequency(int minFrequency)
	{
		for (FragmentPropertySet a : fragmentList)
			a.setMinFrequency(minFrequency);
	}

	public void setSkipOmniFragments(boolean skipOmniFragments)
	{
		for (FragmentPropertySet a : fragmentList)
			a.setSkipOmniFragments(skipOmniFragments);
	}

	public void setMatchEngine(MatchEngine matchEngine)
	{
		for (FragmentPropertySet a : fragmentList)
			a.setMatchEngine(matchEngine);
	}

	public void reset(String showWarningForFile)
	{
		fragmentList.clear();
		AbstractMoleculeProperty.clearPropertyOfType(Fragment.class);

		String files[] = Settings.getFragmentFiles();
		for (String filename : files)
		{
			try
			{
				String warnings = "";
				HashMap<MatchEngine, List<Fragment>> a = new HashMap<MatchEngine, List<Fragment>>();
				for (MatchEngine m : MatchEngine.values())
					a.put(m, new ArrayList<Fragment>());

				CSVFile csv = FileUtil.readCSV(filename);
				for (String[] line : csv.content)
				{
					if (line.length != 2)
						throw new IllegalStateException("Illegal format in line '" + a.get(MatchEngine.CDK).size()
								+ "' (should be <name>,\"<smarts>\"), line:\n" + ArrayUtil.toString(line));
					else
						for (MatchEngine m : MatchEngine.values())
							a.get(m).add(new Fragment(line[0], m, line[1]));

					if (FileUtil.getFilename(filename).equals(showWarningForFile))
						if (!CDKSmartsHandler.isSMARTS(line[1]))
							warnings += "Not a valid SMARTS string: '" + line[1] + "'\n";
				}

				if (warnings.length() > 0)
					JOptionPane.showMessageDialog(Settings.TOP_LEVEL_COMPONENT, "Warnings while parsing SMARTS file '"
							+ showWarningForFile + "':\n" + warnings, "Warnings while parsing SMARTS file",
							JOptionPane.WARNING_MESSAGE);

				String name = "Smarts file: " + FileUtil.getFilename(filename, false);
				String desc = "Num smarts strings: " + a.get(MatchEngine.CDK).size();
				desc += "\nLocation: " + filename;
				String comments = ListUtil.toString(csv.comments, "\n");
				comments = comments.substring(2, comments.length() - 2);
				desc += "\nComments:\n" + comments;
				fragmentList.add(new FragmentSet(name, desc, a));
			}
			catch (Exception e)
			{
				e.printStackTrace();

				if (FileUtil.getFilename(filename).equals(showWarningForFile))
					JOptionPane.showMessageDialog(Settings.TOP_LEVEL_COMPONENT, "Could not parse SMARTS file '"
							+ showWarningForFile + "':\n" + e.getMessage(), "Could not parse SMARTS file",
							JOptionPane.ERROR_MESSAGE);
			}
		}

		for (OBFingerprintSet fp : OBFingerprintSet.FINGERPRINTS)
			fragmentList.add(fp);
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
