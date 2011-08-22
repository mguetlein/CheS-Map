package data;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import main.Settings;

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
				illegalSmarts.add("Smarts: '" + smarts + "', Error: '" + e.getMessage() + "'");
			}
		}

		if (illegalSmarts.size() > 0)
		{
			String s = "Illegal Smarts in " + alert + " :\n";
			for (String ss : illegalSmarts)
				s += ss + "\n";
			JOptionPane.showMessageDialog(Settings.TOP_LEVEL_COMPONENT, s, "Warning", JOptionPane.WARNING_MESSAGE);
		}

		int i = 0;
		for (IMolecule iMolecule : mols)
		{
			try
			{
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
				e.printStackTrace();
				m[i++] = "0";
			}
		}
		return m;
	}
}
