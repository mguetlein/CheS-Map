package org.chesmapper.map.util;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesGenerator;

public class CDKUtil
{
	private static SmilesGenerator gen;

	public static String createSmiles(IAtomContainer compound)
	{
		if (gen == null)
			gen = new SmilesGenerator();
		try
		{
			return gen.create(compound);
		}
		catch (CDKException e)
		{
			return "could not create smiles " + compound;
		}
	}
}
