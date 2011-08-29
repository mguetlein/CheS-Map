package data;

import java.util.ArrayList;
import java.util.List;

import main.TaskProvider;

import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;

import dataInterface.SmartsHandler;

public class CDKSmartsHandler implements SmartsHandler
{
	//	@Override
	//	public boolean isValidSmarts(String smarts)
	//	{
	//		try
	//		{
	//			SMARTSParser.parse(smarts);
	//			return true;
	//		}
	//		catch (Throwable e)
	//		{
	//			return false;
	//		}
	//	}

	@Override
	public String[] match(StructuralAlerts.Alert alert, DatasetFile dataset)
	{
		alert.setNominalDomain(new String[] { "0", "1" });
		IMolecule mols[] = dataset.getMolecules();
		String m[] = new String[mols.length];

		List<String> illegalSmarts = new ArrayList<String>();
		List<SMARTSQueryTool> queryTools = new ArrayList<SMARTSQueryTool>();
		for (String smarts : alert.smarts)
		{
			try
			{
				queryTools.add(new SMARTSQueryTool(smarts));
			}
			catch (Throwable e)
			{
				illegalSmarts.add("Smarts: '" + smarts + "', Error: '" + e.getMessage().replaceAll("\n", ", ") + "'");
			}
		}

		if (illegalSmarts.size() > 0)
		{
			String details = "";
			for (String ss : illegalSmarts)
				details += ss + "\n";
			TaskProvider.task().warning("Illegal Smarts in " + alert, details);
		}

		int i = 0;
		for (IMolecule iMolecule : mols)
		{
			try
			{
				TaskProvider.task().verbose("Matching alert on " + (i + 1) + "/" + mols.length + " compunds");

				boolean match = false;
				for (SMARTSQueryTool queryTool : queryTools)
				{
					if (queryTool.matches(iMolecule))
					{
						match = true;
						break;
					}
				}
				m[i++] = match ? "1" : "0";
			}
			catch (Exception e)
			{
				TaskProvider.task().warning("Could not match molecule", e);
				m[i++] = "0";
			}
		}
		return m;
	}
}
