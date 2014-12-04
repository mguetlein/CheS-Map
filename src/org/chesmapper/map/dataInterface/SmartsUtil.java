package org.chesmapper.map.dataInterface;

import org.chesmapper.map.main.Settings;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.isomorphism.matchers.QueryAtomContainer;
import org.openscience.cdk.smiles.smarts.parser.SMARTSParser;

public class SmartsUtil
{

	public static int getLength(String smarts)
	{
		try
		{
			QueryAtomContainer cont = SMARTSParser.parse(smarts, DefaultChemObjectBuilder.getInstance());
			//			Settings.LOGGER.println("length '" + smarts + "': " + cont.getAtomCount());
			return cont.getAtomCount();
		}
		catch (Throwable e)
		{
			Settings.LOGGER.error(e);
			return -1;
		}
	}

	public static void main(String args[])
	{
		Settings.LOGGER.info(getLength("[#7]-[#6]-[#6]-[#7]:[#7]:[#6]:[#6]"));
		Settings.LOGGER.info(getLength("[a]"));
	}
}
