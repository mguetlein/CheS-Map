package data;

import java.util.ArrayList;
import java.util.List;

import main.Settings;
import main.TaskProvider;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;

import dataInterface.SmartsHandler;

public class CDKSmartsHandler implements SmartsHandler
{
	private static SMARTSQueryTool queryTool;
	static
	{
		try
		{
			queryTool = new SMARTSQueryTool("C");
		}
		catch (CDKException e)
		{
		}
	}

	public static boolean isSMARTS(String smarts)
	{
		try
		{
			queryTool.setSmarts(smarts);
			return true;
		}
		catch (Throwable e)
		{
			return false;
		}
	}

	@Override
	public List<boolean[]> match(List<String> smarts, DatasetFile dataset)
	{
		List<boolean[]> matchList = new ArrayList<boolean[]>();

		boolean validSmarts[] = new boolean[smarts.size()];
		for (int s = 0; s < smarts.size(); s++)
		{
			matchList.add(new boolean[dataset.numCompounds()]);
			try
			{
				queryTool.setSmarts(smarts.get(s));
				validSmarts[s] = true;
			}
			catch (Throwable e)
			{
				Settings.LOGGER.error(e);
				TaskProvider.warning("Illegal Smarts: " + smarts.get(s), e.getMessage().replaceAll("\n", ", "));
			}
		}

		IMolecule mols[] = dataset.getMolecules();
		for (int m = 0; m < mols.length; m++)
		{
			TaskProvider.verbose("Matching smarts on compound " + (m + 1) + "/" + mols.length);
			for (int s = 0; s < smarts.size(); s++)
			{
				if (validSmarts[s])
				{
					//					TaskProvider.task().verbose("Matching smarts " + (s + 1) + "/" + smarts.size());// + " smarts: " + smarts.get(s));
					try
					{
						queryTool.setSmarts(smarts.get(s));
						matchList.get(s)[m] = queryTool.matches(mols[m]);
					}
					catch (Exception e)
					{
						TaskProvider.warning("Could not match molecule", e);
					}
					if (!TaskProvider.isRunning())
						return null;
				}
			}
		}
		return matchList;
	}
}
