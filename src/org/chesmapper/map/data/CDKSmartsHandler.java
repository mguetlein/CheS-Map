package org.chesmapper.map.data;

import java.util.ArrayList;
import java.util.List;

import org.chesmapper.map.dataInterface.SmartsHandler;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;

public class CDKSmartsHandler implements SmartsHandler
{
	private static SMARTSQueryTool queryTool;
	static
	{
		queryTool = new SMARTSQueryTool("C", DefaultChemObjectBuilder.getInstance());
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
	public List<boolean[]> match(List<String> smarts, List<Integer> minNumMatches, DatasetFile dataset)
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

		TaskProvider.debug("Matching smarts on compounds");

		IAtomContainer mols[] = dataset.getCompounds();
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
						matchList.get(s)[m] = queryTool.matches(mols[m]) ? queryTool.countMatches() > minNumMatches
								.get(s) : false;
					}
					catch (Exception e)
					{
						TaskProvider.warning("Could not match compound", e);
					}
					if (!TaskProvider.isRunning())
						return null;
				}
			}
		}
		return matchList;
	}
}
