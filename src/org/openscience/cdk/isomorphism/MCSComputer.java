package org.openscience.cdk.isomorphism;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import main.TaskProvider;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.exception.CDKException;
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
		//		SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
		//		IMolecule mol1 = sp.parseSmiles("c1ccccc1NO");
		//		IMolecule mol2 = sp.parseSmiles("c1cccnc1");
		//		IMolecule mol3 = sp.parseSmiles("c1ccccc1");
		//		//		IMolecule mol1 = sp.parseSmiles("CCCCOOOO");
		//		//		IMolecule mol2 = sp.parseSmiles("CCCCNOO");
		//		//		IMolecule mol3 = sp.parseSmiles("CC");
		//		IMolecule list[] = new IMolecule[] { mol1, mol2, mol3 };
		//		computeMCS(list);

		//mcs: O=C1OC=C(C=C1)C4CCC5(O)(C6CCC3=CC(OC2OC(C)C(O)C(O)C2(O))CCC3(C)C6(CCC45(C))) - O=C1C=CC4(C(=C1)CCC3C5CC(C)C(O)(C(=O)COC2OC(CO)C(O)C(O)C2(O))C5(C)(CC(O)C34(F)))(C)

		Thread th = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					System.out.println("running");
					SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
					IMolecule mol1 = sp
							.parseSmiles("O=C1OC=C(C=C1)C4CCC5(O)(C6CCC3=CC(OC2OC(C)C(O)C(O)C2(O))CCC3(C)C6(CCC45(C)))");
					IMolecule mol2 = sp
							.parseSmiles("O=C1C=CC4(C(=C1)CCC3C5CC(C)C(O)(C(=O)COC2OC(CO)C(O)C(O)C2(O))C5(C)(CC(O)C34(F)))(C)");
					UniversalIsomorphismTester.getOverlaps(mol1, mol2);
					System.out.println("done");
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
		th.start();

		long start = System.currentTimeMillis();
		while (true)
		{
			ThreadUtil.sleep(10000);
			System.out.println(StringUtil.formatTime(System.currentTimeMillis() - start));
		}
	}

	private static void printCandidates(String s, Iterable<IAtomContainer> l)
	{
		System.out.print(s + " candidates: ");
		for (IAtomContainer can : l)
			System.out.print("'" + g.createSMILES(can) + "' ");
		System.out.println();
	}

	public static IAtomContainer computeMCS(IAtomContainer mols[]) throws CDKException
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
			if (TaskProvider.exists())
				TaskProvider.task().verbose(
						"Iterate over compound " + count + "/" + mols.length + ", num MCS-candidates: "
								+ candidates.size());
			if (DEBUG)
				System.out.println("[" + count + "]\nmol: " + g.createSMILES(mol));
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
					if (TaskProvider.exists() && TaskProvider.task().isCancelled())
						return null;
					if (DEBUG)
						System.out.println("mcs: " + g.createSMILES(can) + " - " + g.createSMILES(mol));
					List<IAtomContainer> canMCS = UniversalIsomorphismTester.getOverlaps(can, mol);
					if (DEBUG)
						printCandidates("tmp", canMCS);
					for (IAtomContainer m : canMCS)
						if (m.getAtomCount() > 0)
							newCandiates.add(m);
				}
				candidates = newCandiates;
				if (DEBUG)
					printCandidates("new", candidates);
				if (candidates.size() == 0)
				{
					if (DEBUG)
						System.out.println("nothing found");
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
