package data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import main.Settings;
import main.TaskProvider;
import util.ExternalToolUtil;
import dataInterface.SmartsHandler;

public class OpenBabelSmartsHandler implements SmartsHandler
{

	public static String FP = "FPCHES";

	@Override
	public List<boolean[]> match(List<String> smarts, DatasetFile dataset)
	{
		registerFP();
		createFPFile(smarts);
		return matchSmarts(smarts, dataset);
	}

	private void registerFP()
	{
		String file = "/home/martin/software/openbabel-2.3.0/install/share/openbabel/2.3.0/plugindefines.txt";
		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
			out.write("\n\nPatternFP\n" + FP + "\n" + FP + ".txt\n\n");
			out.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private void createFPFile(List<String> smarts)
	{
		String file = "/home/martin/software/openbabel-2.3.0/install/share/openbabel/2.3.0/" + FP + ".txt";
		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write("#Comments after SMARTS\n");
			int i = 0;
			for (String smart : smarts)
			{
				out.write("  " + i + ":('" + smart + "',0) # " + i + "\n");
				i++;
			}
			out.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private List<boolean[]> matchSmarts(List<String> smarts, DatasetFile dataset)
	{
		List<boolean[]> l = new ArrayList<boolean[]>();
		for (int i = 0; i < smarts.size(); i++)
			l.add(new boolean[dataset.numCompounds()]);

		try
		{
			File f = File.createTempFile("asdfasdf", "asdfasfd");
			String cmd = Settings.BABEL_BINARY.getLocation() + " " + dataset.getSDFPath(false) + " -ofpt -xf" + FP
					+ " -xs";
			TaskProvider.task().verbose("Running babel: " + cmd);
			ExternalToolUtil.run("ob-fingerprints", cmd, f);
			TaskProvider.task().verbose("Parsing smarts");
			BufferedReader buffy = new BufferedReader(new FileReader(f));
			String line = null;
			int compoundIndex = -1;
			while ((line = buffy.readLine()) != null)
			{
				if (line.startsWith(">"))
					compoundIndex++;
				else
				{
					//					System.err.println("frags: " + line);
					boolean minFreq = false;
					for (String s : line.split("\\t"))
					{
						if (s.trim().length() == 0)
							continue;
						if (minFreq && s.matches("^\\*[2-4].*"))
							s = s.substring(2);
						int smartsIndex = Integer.parseInt(s.split(":")[0]);
						l.get(smartsIndex)[compoundIndex] = true;
						minFreq = s.matches(".*>(\\s)*[1-3].*");
					}
				}
			}
		}
		catch (Exception e)
		{
			throw new Error("Error while matching smarts with OpenBabel: " + e.getMessage(), e);
		}
		return l;
	}
}
