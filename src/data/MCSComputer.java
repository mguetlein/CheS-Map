package data;

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
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import dataInterface.ClusterData;
import dataInterface.SubstructureSmartsType;

public class MCSComputer
{
	private static boolean DEBUG = true;

	public static void computeMCS(DatasetFile dataset, List<ClusterData> clusters)
	{
		int count = 0;

		IMolecule allMols[] = dataset.getMolecules(false);

		for (ClusterData c : clusters)
		{
			if (DEBUG)
				System.out.println("\n\n");

			TaskProvider.task().update("Computing MCS for cluster " + (++count) + "/" + clusters.size());

			IMolecule mols[] = new IMolecule[c.getSize()];
			for (int i = 0; i < mols.length; i++)
				mols[i] = allMols[c.getCompounds().get(i).getIndex()];
			IAtomContainer mcsMolecule = null;
			try
			{
				mcsMolecule = computeMCS(mols);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			if (mcsMolecule != null)
			{
				SmilesGenerator g = new SmilesGenerator();
				g.setUseAromaticityFlag(true);
				((ClusterDataImpl) c).setSubstructureSmarts(SubstructureSmartsType.MCS, g.createSMILES(mcsMolecule));
				System.out.println("MCSMolecule: " + c.getSubstructureSmarts(SubstructureSmartsType.MCS));

				//				System.out.println("non aromatic");
				//				g = new SmilesGenerator();
				//				System.out.println(g.createSMILES(mcsMolecule));
			}
			if (DEBUG)
				System.out.println("\n\n");
		}

	}

	public static void main(String[] args) throws CDKException, CloneNotSupportedException, IOException
	{
		SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
		//		IMolecule mol1 = sp.parseSmiles("c1ccccc1NO");
		//		IMolecule mol2 = sp.parseSmiles("c1cccnc1");
		//		IMolecule mol3 = sp.parseSmiles("c1cccoc1");
		IMolecule mol1 = sp.parseSmiles("CCCCOOOO");
		IMolecule mol2 = sp.parseSmiles("CCCCNOO");
		IMolecule mol3 = sp.parseSmiles("CC");
		IMolecule list[] = new IMolecule[] { mol1, mol2, mol3 };
		computeMCS(list);
	}

	public static List<IAtomContainer> computeMCS(IAtomContainer mol1, IAtomContainer mol2) throws CDKException
	{
		return UniversalIsomorphismTester.getOverlaps(mol1, mol2);

		//		List<List<RMap>> maps = UniversalIsomorphismTester.search(mol1, mol2, new BitSet(), new BitSet(), true, false);
		//
		//		////				org.openscience.cdk.smsd.Isomorphism mcs = new org.openscience.cdk.smsd.Isomorphism(
		//		////						org.openscience.cdk.smsd.interfaces.Algorithm.DEFAULT, true);
		//		////				mcs.init(mol1, mol2, true, true);
		//		////				mcs.setChemFilters(true, true, true);
		//		////				List<Map<Integer, Integer>> maps = mcs.getAllMapping();
		//
		//		//		List<List<RMap>> maps = UniversalIsomorphismTester.getSubgraphMaps(mol1, mol2);
		//		//
		//		List<IAtomContainer> mols = new ArrayList<IAtomContainer>();
		//		//for (Map<Integer, Integer> map : maps)
		//		for (List<RMap> map : maps)
		//		{
		//			System.err.println("map size: " + map.size());
		//			if (map.size() == 0)
		//				throw new Error();
		//
		//			IMolecule mcsMolecule = DefaultChemObjectBuilder.getInstance().newInstance(IMolecule.class, mol1);
		//			List<IAtom> atomsToBeRemoved = new ArrayList<IAtom>();
		//			for (IAtom atom : mcsMolecule.atoms())
		//			{
		//				int index = mcsMolecule.getAtomNumber(atom);
		//				//				if (!map.containsKey(index))
		//				//atomsToBeRemoved.add(atom);
		//
		//				boolean match = false;
		//				for (RMap rMap : map)
		//					if (rMap.getId1() == index)
		//					{
		//						match = true;
		//						break;
		//					}
		//				if (!match)
		//					atomsToBeRemoved.add(atom);
		//			}
		//			for (IAtom atom : atomsToBeRemoved)
		//				mcsMolecule.removeAtomAndConnectedElectronContainers(atom);
		//			if (mcsMolecule.getAtomCount() == 0)
		//				throw new Error();
		//			boolean isomorph = false;
		//			for (IAtomContainer mol : mols)
		//				if (UniversalIsomorphismTester.isIsomorph(mol, mcsMolecule))
		//				{
		//					isomorph = true;
		//					break;
		//				}
		//			if (!isomorph)
		//				mols.add(mcsMolecule);
		//		}
		//		return mols;
	}

	private static void printCandidates(String s, Iterable<IAtomContainer> l)
	{
		SmilesGenerator g = new SmilesGenerator();
		System.out.print(s + " candidates: ");
		for (IAtomContainer can : l)
			System.out.print("'" + g.createSMILES(can) + "' ");
		System.out.println();
	}

	public static IAtomContainer computeMCS(IAtomContainer mols[]) throws CDKException
	{
		Arrays.sort(mols, new Comparator<IAtomContainer>()
		{
			@Override
			public int compare(IAtomContainer o1, IAtomContainer o2)
			{
				return new Integer(o1.getAtomCount()).compareTo(new Integer(o2.getAtomCount()));
			}
		});

		SmilesGenerator g = new SmilesGenerator();
		g.setUseAromaticityFlag(true);
		List<IAtomContainer> candidates = new ArrayList<IAtomContainer>();
		int count = 0;
		for (IAtomContainer mol : mols)
		{
			mol = new Molecule(AtomContainerManipulator.removeHydrogens(mol));
			TaskProvider.task()
					.verbose(
							"Iterate over compound " + count + "/" + mols.length + ", num MCS-candidates: "
									+ candidates.size());
			if (DEBUG)
				System.out.println("[" + count + "]\nmol: " + g.createSMILES(mol));
			if (candidates.size() == 0)
			{
				candidates.add(mol);
			}
			else
			{
				List<IAtomContainer> newCandiates = new ArrayList<IAtomContainer>();
				for (IAtomContainer can : candidates)
				{
					if (TaskProvider.task().isCancelled())
						return null;

					if (DEBUG)
						System.out.println("mcs: " + g.createSMILES(can) + " - " + g.createSMILES(mol));
					List<IAtomContainer> canMCS = computeMCS(can, mol);
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

	//	public static void fail_snippet() throws InvalidSmilesException, CDKException
	//	{
	//		SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
	//		IMolecule mol1 = sp.parseSmiles("c1ccccc1NO");
	//		IMolecule mol2 = sp.parseSmiles("c1ccccc1");
	//		IMolecule mol3 = sp.parseSmiles("c1cccnc1");
	//		IMolecule mol4 = sp.parseSmiles("c1cccoc1");
	//		IMolecule mols[] = new IMolecule[] { mol1, mol2, mol3, mol4 };
	//		SmilesGenerator g = new SmilesGenerator();
	//		g.setUseAromaticityFlag(true);
	//
	//		List<IAtomContainer> candidates = new ArrayList<IAtomContainer>();
	//		int count = 0;
	//		for (IAtomContainer mol : mols)
	//		{
	//			mol = new Molecule(AtomContainerManipulator.removeHydrogens(mol));
	//			System.out.println("[" + count + "]\nmol: " + g.createSMILES(mol));
	//			if (candidates.size() == 0)
	//			{
	//				candidates.add(mol);
	//			}
	//			else
	//			{
	//				List<IAtomContainer> newCandiates = new ArrayList<IAtomContainer>();
	//				for (IAtomContainer can : candidates)
	//				{
	//					System.out.println("mcs: " + g.createSMILES(can) + " - " + g.createSMILES(mol));
	//					List<IAtomContainer> canMCS = computeMCS(can, mol);
	//					for (IAtomContainer m : canMCS)
	//						if (m.getAtomCount() > 0)
	//						{
	//							System.out.println("new candidate: '" + g.createSMILES(m) + "'");
	//							newCandiates.add(m);
	//						}
	//				}
	//				candidates = newCandiates;
	//
	//				if (candidates.size() == 0)
	//				{
	//					System.out.println("nothing found");
	//					break;
	//				}
	//			}
	//			System.out.print("candidates: ");
	//			for (IAtomContainer can : candidates)
	//				System.out.print("'" + g.createSMILES(can) + "' ");
	//			System.out.println();
	//			count++;
	//		}
	//	}
}
