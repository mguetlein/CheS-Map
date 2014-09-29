package main.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

public class CDKTest
{
	public static void main(String args[]) throws Exception
	{
		//		try
		//		{
		//			SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
		//			IMolecule m = sp.parseSmiles("N[C@@H](C)C(=O)O");
		//
		//			CDKHydrogenAdder ha = CDKHydrogenAdder.getInstance(DefaultChemObjectBuilder.getInstance());
		//			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(m);
		//			ha.addImplicitHydrogens(m);
		//			AtomContainerManipulator.convertImplicitToExplicitHydrogens(m);
		//
		//			Settings.LOGGER.info(m);
		//
		//			IMolecularDescriptor x = new XLogPDescriptor();
		//			Settings.LOGGER.info("xlogp " + x.calculate(m).getValue());
		//
		//			x = new WeightDescriptor();
		//			Settings.LOGGER.info("weight " + x.calculate(m).getValue());
		//
		//			x = new WienerNumbersDescriptor();
		//			Settings.LOGGER.info("wiener " + x.calculate(m).getValue());
		//
		//			x = new BCUTDescriptor();
		//			Settings.LOGGER.info("bcut " + x.calculate(m).getValue());
		//		}
		//		catch (InvalidSmilesException e)
		//		{
		//			Settings.LOGGER.error(e);
		//		}
		//		catch (CDKException e)
		//		{
		//			Settings.LOGGER.error(e);
		//		}

		String smiles[] = { "c1ccccc1", "C1=CC=CC=C1" };

		SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
		IAtomContainer mols[] = new IAtomContainer[] { sp.parseSmiles(smiles[0]), sp.parseSmiles(smiles[1]) };

		System.out.println("parse from smiles");
		for (int i = 0; i < mols.length; i++)
			System.out.println(smiles[i] + " aromtic ? " + mols[i].getAtom(0).getFlag(CDKConstants.ISAROMATIC));

		File tmp = File.createTempFile("file", "sdf");
		SDFWriter writer = new SDFWriter(new FileOutputStream(tmp));
		for (int i = 0; i < mols.length; i++)
		{
			//			FixBondOrdersTool fbot = new FixBondOrdersTool();
			//			mols[i] = fbot.kekuliseAromaticRings((IMolecule) mols[i]);
			mols[i] = mykekule(mols[i]);

			writer.write(mols[i]);
		}
		writer.close();

		ISimpleChemObjectReader reader = new ReaderFactory().createReader(new InputStreamReader(
				new FileInputStream(tmp)));
		IChemFile content = (IChemFile) reader.read((IChemObject) new ChemFile());
		List<IAtomContainer> sdfMols = ChemFileManipulator.getAllAtomContainers(content);

		System.out.println("\nwrite and read from sdf");
		for (int i = 0; i < sdfMols.size(); i++)
			System.out.println(smiles[i] + " aromtic ? " + sdfMols.get(i).getAtom(0).getFlag(CDKConstants.ISAROMATIC));

		for (int i = 0; i < sdfMols.size(); i++)
		{
			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(sdfMols.get(i));
			CDKHueckelAromaticityDetector.detectAromaticity(sdfMols.get(i));
		}

		System.out.println("\ndetect aromaticity from sdf");
		for (int i = 0; i < sdfMols.size(); i++)
			System.out.println(smiles[i] + " aromtic ? " + sdfMols.get(i).getAtom(0).getFlag(CDKConstants.ISAROMATIC));
	}

	static IAtomContainer mykekule(IAtomContainer org) throws Exception
	{

		//		final IChemObjectBuilder bldr = SilentChemObjectBuilder.getInstance();
		//		final SmilesParser smipar = new SmilesParser(bldr);
		//		final SmilesGenerator smigen = SmilesGenerator.unique();
		//
		//		final int n = org.getAtomCount();
		//		int[] ordering = new int[n];
		//
		//		// generate a kekule assignment via SMILES and store the output order (a permutation of
		//		// atom indices) 
		//		IAtomContainer cpy = smipar.parseSmiles(smigen.create(org, ordering));
		//
		//		// index atoms for lookup
		//		final Map<IAtom, Integer> atomIndexMap = new HashMap<>();
		//		for (IAtom atom : org.atoms())
		//			atomIndexMap.put(atom, atomIndexMap.size());
		//
		//		// util to get atom index -> bond map
		//		EdgeToBondMap bondMap = EdgeToBondMap.withSpaceFor(cpy);
		//		GraphUtil.toAdjList(cpy, bondMap);
		//
		//		for (IBond bond : org.bonds())
		//		{
		//
		//			// atom indices
		//			int u = atomIndexMap.get(bond.getAtom(0));
		//			int v = atomIndexMap.get(bond.getAtom(1));
		//
		//			// atom indices in 'cpy'
		//			int uCpy = ordering[u];
		//			int vCpy = ordering[v];
		//
		//			// propagate the assigned bond order
		//			bond.setOrder(bondMap.get(uCpy, vCpy).getOrder());
		//
		//			// note the following would also work to get the cpy bond
		//			// cpy.getBond(cpy.getAtom(uCpy), cpy.getAtom(vCpy));
		//		}

		return org;
	}
}
