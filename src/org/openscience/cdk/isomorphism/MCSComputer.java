package org.openscience.cdk.isomorphism;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import main.ScreenSetup;
import main.Settings;
import main.TaskProvider;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import util.StringUtil;
import util.ThreadUtil;

public class MCSComputer
{
	public static boolean DEBUG = false;
	private static SmilesGenerator g;
	static
	{
		g = new SmilesGenerator(true, true);
		g.setUseAromaticityFlag(true);
	}

	public static void main(String[] args) throws CDKException, CloneNotSupportedException, IOException
	{
		ScreenSetup.INSTANCE = ScreenSetup.DEFAULT;

		SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
		IMolecule mol1 = sp.parseSmiles("O=C(OC(COc1ccccc1CC=C)CNC(C)C)C2CC2");
		IMolecule mol2 = sp.parseSmiles("O=C(OC1CC3N(C)C(C1)C2OC23)C(c4ccccc4)CO");
		IMolecule mol3 = sp.parseSmiles("O=C(OC1CC3N(CCCCC)C(C1)C2OC23)C(c4cccnc4)CO");
		//		IMolecule mol1 = sp.parseSmiles("Oc1ccccc1");
		//		IMolecule mol2 = sp.parseSmiles("OCCCCCC");

		//COCCCNC(C)C
		//IMolecule mol3 = sp.parseSmiles("c1ccccc1");
		//		IMolecule mol1 = sp.parseSmiles("CCCCOOOO");
		//		IMolecule mol2 = sp.parseSmiles("CCCCNOO");
		//		IMolecule mol3 = sp.parseSmiles("CC");
		IMolecule list[] = new IMolecule[] { mol1, mol2, mol3 };
		computeMCS(list);
		System.exit(1);

		//mcs: O=C1OC=C(C=C1)C4CCC5(O)(C6CCC3=CC(OC2OC(C)C(O)C(O)C2(O))CCC3(C)C6(CCC45(C))) - O=C1C=CC4(C(=C1)CCC3C5CC(C)C(O)(C(=O)COC2OC(CO)C(O)C(O)C2(O))C5(C)(CC(O)C34(F)))(C)

		Thread th = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					Settings.LOGGER.info("running");
					SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
					IMolecule mol1 = sp
							.parseSmiles("O=C1OC=C(C=C1)C4CCC5(O)(C6CCC3=CC(OC2OC(C)C(O)C(O)C2(O))CCC3(C)C6(CCC45(C)))");
					IMolecule mol2 = sp
							.parseSmiles("O=C1C=CC4(C(=C1)CCC3C5CC(C)C(O)(C(=O)COC2OC(CO)C(O)C(O)C2(O))C5(C)(CC(O)C34(F)))(C)");
					UniversalIsomorphismTester.getOverlaps(mol1, mol2);
					Settings.LOGGER.info("done");
				}
				catch (Exception e)
				{
					Settings.LOGGER.error(e);
				}
			}
		});
		th.start();

		long start = System.currentTimeMillis();
		while (true)
		{
			ThreadUtil.sleep(10000);
			Settings.LOGGER.info(StringUtil.formatTime(System.currentTimeMillis() - start));
		}
	}

	private static void printCandidates(String s, Iterable<IAtomContainer> l)
	{
		Settings.LOGGER.info(s + " candidates: ");
		for (IAtomContainer can : l)
		{
			int aromCan = 0;
			for (IAtom atom : can.atoms())
				if (atom.getFlag(CDKConstants.ISAROMATIC))
					aromCan++;
			Settings.LOGGER.info("'" + g.createSMILES(can) + " (#arom:" + aromCan + ")' ");
		}
		Settings.LOGGER.info();
	}

	public static IAtomContainer computeMCS(IAtomContainer mols[]) throws CDKException
	{
		return computeMCS(mols, 2);
	}

	public static IAtomContainer computeMCS(IAtomContainer mols[], int minNumAtoms) throws CDKException
	{
		// sort according to number of atoms in ascending order to reduce computational effort
		Arrays.sort(mols, new Comparator<IAtomContainer>()
		{
			@Override
			public int compare(IAtomContainer o1, IAtomContainer o2)
			{
				return new Integer(o1.getAtomCount()).compareTo(new Integer(o2.getAtomCount()));
			}
		});

		List<IAtomContainer> candidates = new ArrayList<IAtomContainer>();
		int count = 0;
		// iterate over compounds
		for (IAtomContainer mol : mols)
		{
			mol = new Molecule(AtomContainerManipulator.removeHydrogens(mol));
			TaskProvider.verbose("Iterate over compound " + count + "/" + mols.length + ", num MCS-candidates: "
					+ candidates.size());
			if (DEBUG)
				Settings.LOGGER.info("[" + count + "]\nmol: " + g.createSMILES(mol));
			if (candidates.size() == 0)
			{
				// if == first compound, add compound to candidates
				candidates.add(mol);
			}
			else
			{
				// else compute mcs of all candidates with this compound, use results as new candidates 
				List<IAtomContainer> newCandiates = new ArrayList<IAtomContainer>();
				for (IAtomContainer can : candidates)
				{
					if (!TaskProvider.isRunning())
						return null;
					if (DEBUG)
					{
						int aromCan = 0;
						for (IAtom atom : can.atoms())
							if (atom.getFlag(CDKConstants.ISAROMATIC))
								aromCan++;
						int aromMol = 0;
						for (IAtom atom : mol.atoms())
							if (atom.getFlag(CDKConstants.ISAROMATIC))
								aromMol++;
						Settings.LOGGER.info("mcs: " + g.createSMILES(can) + " (#arom:" + aromCan + ") - "
								+ g.createSMILES(mol) + " (#arom:" + aromMol + ")");
					}
					List<IAtomContainer> canMCS = MyUniversalIsomorphismTester.getOverlaps(can, mol);
					if (DEBUG)
						printCandidates("tmp", canMCS);
					for (IAtomContainer m : canMCS)
						if (m.getAtomCount() >= minNumAtoms)
							newCandiates.add(m);
				}
				candidates = newCandiates;
				if (DEBUG)
					printCandidates("new", candidates);
				if (candidates.size() == 0)
				{
					if (DEBUG)
						Settings.LOGGER.info("nothing found");
					break;
				}
			}
			count++;
		}
		if (DEBUG)
			printCandidates("final", candidates);
		IAtomContainer max = null;
		for (IAtomContainer mol : candidates)
			if (max == null || mol.getAtomCount() > max.getAtomCount())
				max = mol;
		return max;
	}
}
