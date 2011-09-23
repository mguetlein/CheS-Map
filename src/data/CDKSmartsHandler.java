package data;

import java.util.ArrayList;
import java.util.List;

import main.TaskProvider;

import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;

import dataInterface.SmartsHandler;

public class CDKSmartsHandler implements SmartsHandler
{
	@Override
	public List<boolean[]> match(List<String> smarts, DatasetFile dataset)
	{
		List<boolean[]> l = new ArrayList<boolean[]>();
		int count = 0;
		for (String smart : smarts)
		{
			TaskProvider.task().update("Matching " + (count + 1) + "/" + smarts.size() + " smarts: " + smart);

			IMolecule mols[] = dataset.getMolecules();
			boolean m[] = new boolean[mols.length];

			SMARTSQueryTool queryTool = null;
			try
			{
				queryTool = new SMARTSQueryTool(smart);
			}
			catch (Throwable e)
			{
				e.printStackTrace();
				TaskProvider.task().warning("Illegal Smarts: " + smart, e.getMessage().replaceAll("\n", ", "));
			}

			int i = 0;
			for (IMolecule iMolecule : mols)
			{
				try
				{
					TaskProvider.task().verbose("Matching smarts on " + (i + 1) + "/" + mols.length + " compunds");
					boolean match = queryTool != null && queryTool.matches(iMolecule);
					m[i++] = match;
				}
				catch (Exception e)
				{
					TaskProvider.task().warning("Could not match molecule", e);
					m[i++] = false;
				}
			}
			l.add(m);
			count++;
		}
		return l;
	}
}
