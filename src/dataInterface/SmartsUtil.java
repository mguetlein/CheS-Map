package dataInterface;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.isomorphism.matchers.QueryAtomContainer;
import org.openscience.cdk.smiles.smarts.parser.SMARTSParser;

public class SmartsUtil
{

	public static int getLength(String smarts)
	{
		try
		{
			QueryAtomContainer cont = SMARTSParser.parse(smarts);
			//			System.out.println("length '" + smarts + "': " + cont.getAtomCount());
			return cont.getAtomCount();
		}
		catch (CDKException e)
		{
			e.printStackTrace();
			return -1;
		}
	}

	public static void main(String args[])
	{
		System.out.println(getLength("[#7]-[#6]-[#6]-[#7]:[#7]:[#6]:[#6]"));
	}
}
