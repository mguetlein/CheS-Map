package data;

import io.JarUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import main.Settings;
import util.ArrayUtil;
import util.FileUtil;
import util.FileUtil.CSVFile;
import util.ListUtil;
import dataInterface.AbstractMoleculeProperty;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculePropertySet;

public class StructuralAlerts
{
	public static class Alert extends AbstractMoleculeProperty implements MoleculePropertySet
	{
		String name;
		String name2;
		String[] smarts;

		public Alert(String name, String name2, String[] smarts)
		{
			this.name = name;
			this.name2 = name2;
			this.smarts = smarts;

			setTypeAllowed(Type.NUMERIC, false);
			setType(Type.NOMINAL);
		}

		@Override
		public int getSize()
		{
			return 1;
		}

		@Override
		public MoleculeProperty get(int index)
		{
			if (index != 0)
				throw new Error("only one prop available");
			return this;
		}

		public String toString()
		{
			String s = name;
			if (name2 != null && name2.trim().length() > 0)
				s += " (" + name2 + ")";
			return s;
		}

		//		public boolean equals(Object o)
		//		{
		//			return o instanceof Alert && o.toString().equals(toString());
		//		}

		public String getDescription()
		{
			return toString() + "\n" + ArrayUtil.toString(smarts);
		}
	}

	List<String> names = new ArrayList<String>();
	List<List<Alert>> alerts = new ArrayList<List<Alert>>();
	List<String> description = new ArrayList<String>();

	public static StructuralAlerts instance = new StructuralAlerts();

	private StructuralAlerts()
	{
		JarUtil.extractFromJAR("structural_alerts/DNA-SMARTS.csv", Settings.getAlertFileDestination("DNA-SMARTS.csv"),
				false);
		JarUtil.extractFromJAR("structural_alerts/Phospholipidosis.csv",
				Settings.getAlertFileDestination("Phospholipidosis.csv"), false);

		reset();
	}

	public void reset()
	{
		names.clear();
		alerts.clear();

		String files[] = Settings.getAlertFiles();
		for (String filename : files)
		{
			try
			{
				String warnings = "";

				List<Alert> a = new ArrayList<Alert>();
				CSVFile csv = FileUtil.readCSV(filename);
				for (String[] line : csv.content)
				{
					if (line.length < 3)
						throw new IllegalStateException("no smarts csv: " + ArrayUtil.toString(line));
					else
						a.add(new Alert(line[0], line[1], Arrays.copyOfRange(line, 2, line.length)));
				}

				System.err.println(warnings);
				names.add(FileUtil.getFilename(filename, false));
				alerts.add(a);
				String desc = ListUtil.toString(csv.comments, "\n");
				desc = desc.substring(2, desc.length() - 2);
				description.add(filename + "\n\n" + desc);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public int getNumSets()
	{
		return alerts.size();
	}

	public String getSetName(int i)
	{
		return names.get(i);
	}

	public String getDescription(int i)
	{
		return description.get(i);
	}

	public MoleculePropertySet[] getSet(int i)
	{
		Alert a[] = new Alert[alerts.get(i).size()];
		return alerts.get(i).toArray(a);
	}

	public Alert findFromString(String string)
	{
		for (List<Alert> a : alerts)
		{
			for (Alert alert : a)
			{
				if (alert.toString().equals(string))
					return alert;
			}
		}
		return null;
	}

	public String getDescriptionForName(String name)
	{
		int index = names.indexOf(name);
		if (index == -1)
			return null;
		else
			return getDescription(index);
	}
}
