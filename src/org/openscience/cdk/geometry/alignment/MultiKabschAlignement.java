package org.openscience.cdk.geometry.alignment;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.vecmath.Point3d;

import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.StringUtil;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.isomorphism.MyUniversalIsomorphismTester;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.isomorphism.mcss.RMap;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.modeling.builder3d.ModelBuilder3D;
import org.openscience.cdk.modeling.builder3d.TemplateHandler3D;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.smiles.smarts.SMARTSQueryTool;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

public class MultiKabschAlignement
{
	public static boolean DEBUG = false;

	private static SMARTSQueryTool queryTool;
	static
	{
		//		try
		//		{
		queryTool = new SMARTSQueryTool("C", DefaultChemObjectBuilder.getInstance());
		//		}
		//		catch (CDKException e)
		//		{
		//		}
	}
	private static SmilesGenerator g;
	static
	{
		g = new SmilesGenerator(); //true, true);
		g.setUseAromaticityFlag(true);
	}

	static class MoleculeInfo
	{
		List<Atom[]> smartsMatchAtoms = new ArrayList<Atom[]>();
		List<Set<IBond>> smartsMatchBonds = new ArrayList<Set<IBond>>();

		public int numSmartsMatches()
		{
			return smartsMatchAtoms.size();
		}
	}

	Random r = new Random();

	public static void align(IAtomContainer[] molecules, String smarts) throws CDKException, CloneNotSupportedException
	{
		MoleculeInfo molInfos[] = new MoleculeInfo[molecules.length];

		for (int m = 0; m < molecules.length; m++)
		{
			molInfos[m] = new MoleculeInfo();
			queryTool.setSmarts(smarts);
			if (!queryTool.matches(molecules[m]))
				throw new IllegalStateException(g.create(molecules[m]) + " does not match " + smarts);

			boolean warning = false;
			for (IAtom a : molecules[m].atoms())
			{
				if (a.getPoint3d() == null && !warning)
				{
					if (a.getPoint2d() == null)
						throw new Error("no 2d coordinates");
					Settings.LOGGER.warn("no 3d coordinates available for " + g.create(molecules[m]));
					warning = true;
				}
				if (a.getPoint3d() == null)
					a.setPoint3d(new Point3d(a.getPoint2d().x, a.getPoint2d().y, 0));
			}

			List<List<Integer>> matchingAtoms = queryTool.getMatchingAtoms();
			for (List<Integer> tmpMatchingAtoms : matchingAtoms)
			{
				Atom[] tmpAtoms = new Atom[tmpMatchingAtoms.size()];
				for (int i = 0; i < tmpAtoms.length; i++)
					tmpAtoms[i] = (Atom) molecules[m].getAtom(tmpMatchingAtoms.get(i));
				if (tmpAtoms.length == 0)
					throw new IllegalStateException();
				if (molInfos[m].smartsMatchAtoms.size() > 0
						&& tmpAtoms.length != molInfos[m].smartsMatchAtoms.get(0).length)
					throw new IllegalStateException();
				molInfos[m].smartsMatchAtoms.add(tmpAtoms);

				Set<IBond> tmpBonds = new HashSet<IBond>();
				for (int i = 0; i < tmpAtoms.length; i++)
					for (int j = 0; j < tmpAtoms.length; j++)
						if (molecules[m].getBond(tmpAtoms[i], tmpAtoms[j]) != null)
							tmpBonds.add(molecules[m].getBond(tmpAtoms[i], tmpAtoms[j]));
				molInfos[m].smartsMatchBonds.add(tmpBonds);
			}
		}

		int selectedMatchIndex1 = -1;
		Point3d centerOfMass = null;

		IAtomContainer mol1 = molecules[0];
		MoleculeInfo molInfo1 = molInfos[0];

		if (DEBUG)
			Settings.LOGGER.info("Num matches in compound 1: " + molInfo1.numSmartsMatches());
		TaskProvider.debug("Align compound " + molInfos.length + " with SMARTS " + smarts);

		for (int m = 1; m < molecules.length; m++)
		{
			IAtomContainer mol2 = molecules[m];
			MoleculeInfo molInfo2 = molInfos[m];

			double rmsd = Double.MAX_VALUE;
			double completeRMSD = Double.MAX_VALUE;
			Atom bestAtoms1[] = null;
			Atom bestAtoms2[] = null;
			int bestMol1Index = -1;

			TaskProvider.verbose("Align compound " + (m + 1) + "/" + molInfos.length + " to first compound");
			if (DEBUG)
			{
				Settings.LOGGER.info("Num matches in compound " + (m + 1) + ": " + molInfo2.numSmartsMatches());
				Settings.LOGGER.info("Aligning:");
			}

			for (int matchIndex1 = 0; matchIndex1 < molInfo1.numSmartsMatches(); matchIndex1++)
			{
				if (selectedMatchIndex1 != -1 && selectedMatchIndex1 != matchIndex1)
					continue;

				Set<IBond> bonds1 = molInfo1.smartsMatchBonds.get(matchIndex1);

				for (int matchIndex2 = 0; matchIndex2 < molInfo2.numSmartsMatches(); matchIndex2++)
				{
					Set<IBond> bonds2 = molInfo2.smartsMatchBonds.get(matchIndex2);
					List<Atom[]> atoms1 = new ArrayList<Atom[]>();
					List<Atom[]> atoms2 = new ArrayList<Atom[]>();
					boolean isomorph = true;

					List<List<RMap>> bMaps = MyUniversalIsomorphismTester.getIsomorphMaps(mol1,
							ArrayUtil.toArray(bonds1), mol2, ArrayUtil.toArray(bonds2));

					if (bMaps == null || bMaps.size() == 0 || bMaps.get(0).size() == 0)
					{
						isomorph = false;
						atoms1.add(molInfo1.smartsMatchAtoms.get(matchIndex1));
						atoms2.add(molInfo2.smartsMatchAtoms.get(matchIndex2));
					}
					else
					{
						for (List<RMap> bMap : bMaps)
						{
							if (bonds1.size() != bMap.size())
								throw new IllegalStateException();
							//matches are isomorph, convert to atom matches
							List<RMap> aMap = UniversalIsomorphismTester.makeAtomsMapOfBondsMap(bMap, mol1, mol2);

							Atom[] a1 = new Atom[aMap.size()];
							Atom[] a2 = new Atom[aMap.size()];
							for (int i = 0; i < a1.length; i++)
							{
								a1[i] = (Atom) mol1.getAtom(aMap.get(i).getId1());
								a2[i] = (Atom) mol2.getAtom(aMap.get(i).getId2());
							}
							atoms1.add(a1);
							atoms2.add(a2);
							if (a1.length != molInfo1.smartsMatchAtoms.get(0).length)
								throw new IllegalStateException("isomorph " + a1.length + " != matching: "
										+ molInfo1.smartsMatchAtoms.get(0).length);
						}
					}

					if (atoms1.size() == 0)
						throw new IllegalStateException();

					for (int mappingIndex = 0; mappingIndex < atoms1.size(); mappingIndex++)
					{
						Atom[] a1 = atoms1.get(mappingIndex);
						Atom[] a2 = atoms2.get(mappingIndex);

						for (int i = 0; i < a2.length; i++)
							if (isomorph && a1[i].getAtomicNumber() != a2[i].getAtomicNumber())
								throw new IllegalArgumentException("isomorph but not the same atom number");

						KabschAlignment tmpKa = new KabschAlignment(a1, a2);
						tmpKa.align();
						double tmpRMSD = tmpKa.getRMSD();
						//						if (Double.isNaN(tmpRMSD) || Double.isInfinite(tmpRMSD))
						//							throw new IllegalStateException("rmsd is not valid " + a1.length);
						double tmpCompleteRMSD = Double.MAX_VALUE;
						if (DEBUG)
							Settings.LOGGER.info(StringUtil.formatDouble(tmpRMSD) + " ");

						if (tmpRMSD - 0.1 < rmsd)
						{
							//compute complete rmsd
							IAtomContainer mol1clone = (IAtomContainer) mol1.clone();
							IAtomContainer mol2clone = (IAtomContainer) mol2.clone();
							Point3d cm1 = tmpKa.getCenterOfMass();
							for (int i = 0; i < mol1clone.getAtomCount(); i++)
							{
								Atom a = (Atom) mol1clone.getAtom(i);
								a.setPoint3d(new Point3d(a.getPoint3d().x - cm1.x, a.getPoint3d().y - cm1.y, a
										.getPoint3d().z - cm1.z));
							}
							tmpKa.rotateAtomContainer(mol2clone);
							tmpCompleteRMSD = getAllAtomRMSD(mol1clone, mol2clone);
							//compute complete rmsd done

							boolean improvement = (tmpRMSD + 0.1 < rmsd) || (tmpCompleteRMSD + 0.1 < completeRMSD);
							if (improvement)
							{
								rmsd = tmpRMSD;
								completeRMSD = tmpCompleteRMSD;
								bestAtoms1 = a1;
								bestAtoms2 = a2;
								bestMol1Index = matchIndex1;
							}
						}
					}
				}
			}
			if (DEBUG)
				Settings.LOGGER.info();

			if (bestAtoms1 == null)
				throw new IllegalStateException("Kabsch Alignement failed to align according to smarts '" + smarts
						+ "'");

			selectedMatchIndex1 = bestMol1Index;

			KabschAlignment ka = new KabschAlignment(bestAtoms1, bestAtoms2);
			ka.align();
			if (centerOfMass != null && !centerOfMass.equals(ka.getCenterOfMass()))
				Settings.LOGGER.warn("Center of mol1 is not equal for all alignments:\n" + centerOfMass + " != "
						+ ka.getCenterOfMass());
			centerOfMass = ka.getCenterOfMass();

			//translate only once
			if (m == 1)
			{
				Point3d cm1 = centerOfMass;
				for (int i = 0; i < mol1.getAtomCount(); i++)
				{
					Atom a = (Atom) mol1.getAtom(i);
					a.setPoint3d(new Point3d(a.getPoint3d().x - cm1.x, a.getPoint3d().y - cm1.y, a.getPoint3d().z
							- cm1.z));
				}
			}

			HashMap<Integer, Integer> mappedAtoms = null;
			if (DEBUG)
			{
				mappedAtoms = new HashMap<Integer, Integer>();
				for (int atomIndex = 0; atomIndex < bestAtoms1.length; atomIndex++)
					mappedAtoms.put(mol1.getAtomNumber(bestAtoms1[atomIndex]),
							mol2.getAtomNumber(bestAtoms2[atomIndex]));
				Settings.LOGGER.info("RMSD between matched subgraphs BEFORE aligning "
						+ GeometryTools.getAllAtomRMSD(mol1, mol2, mappedAtoms, true));
				Settings.LOGGER.info("RMSD between whole compounds   BEFORE aligning " + getAllAtomRMSD(mol1, mol2));

			}
			ka.rotateAtomContainer(mol2);
			double rmsdFinal = ka.getRMSD();
			if (rmsd != rmsdFinal)
				throw new IllegalStateException();
			if (DEBUG)
			{
				double rmsdAfter = GeometryTools.getAllAtomRMSD(mol1, mol2, mappedAtoms, true);
				Settings.LOGGER.info("RMSD between matched subgraphs AFTER  aligning " + rmsdAfter);
				Settings.LOGGER.info("RMSD between whole compounds   AFTER  aligning " + getAllAtomRMSD(mol1, mol2));
			}
		}

	}

	public static double getAllAtomRMSD(IAtomContainer mol1, IAtomContainer mol2) throws CDKException
	{
		double sum = 0;
		double RMSD;
		int n = 0;
		for (IAtom a : mol1.atoms())
		{
			double min = Double.MAX_VALUE;
			for (IAtom b : mol2.atoms())
			{
				double dist = Math.pow(a.getPoint3d().distance(b.getPoint3d()), 2);
				if (dist < min)
					min = dist;
			}
			sum += min;
			n++;
		}
		for (IAtom b : mol2.atoms())
		{
			double min = Double.MAX_VALUE;
			for (IAtom a : mol1.atoms())
			{
				double dist = Math.pow(a.getPoint3d().distance(b.getPoint3d()), 2);
				if (dist < min)
					min = dist;
			}
			sum += min;
			n++;
		}
		RMSD = Math.sqrt(sum / n);
		return RMSD;
	}

	public static void main(String args[])
	{
		DEBUG = true;

		try
		{
			//			String s[] = { "c1cc(ccc1Oc2cc(cc(c2)Br)Br)Br", "c2cc(Oc1ccc(cc1Br)Br)c(cc2Br)Br" };
			//			String smarts = "c1ccc(cc1)Oc2ccc(cc2)Br";
			//			MultiKabschAlignement.align("pbde", s, smarts);
			//			Settings.LOGGER.println();

			String s2[] = { "CCCC1CCNC(=O)C1", "O=C1CCC2CCCCC2(N1)" };
			String smarts2 = "CCCCCCNC(C)=O";
			MultiKabschAlignement.align("not-isomorph", s2, smarts2);
			Settings.LOGGER.info();
			//
			//			String s3[] = { "O=S(=O)(N)c1ccc(cc1)n3ncc(c3(c2ccccc2))Cl",
			//					"O=Cc3cc(c1ccc(cc1)S(=O)(=O)C)n(c2ccc(F)cc2)c3C",
			//					"O=S(=O)(N)c1ccc(cc1)n3nc(cc3(c2ccc(cc2)C))C(F)(F)F",
			//					"O=S(=O)(N)c1ccc(cc1)n3nc(cc3(c2cc(F)c(OC)c(F)c2))C(F)F" };
			//			String smarts3 = "ccc(nc1ccccc1)c2ccccc2";
			//			MultiKabschAlignement.align("cox", s3, smarts3);
			//			Settings.LOGGER.println();
			//			
			//			String s4[] = { "O=S(=O)(c1ccc(cc1)C3=C(c2ccc(F)c(F)c2)CCC3)C",
			//					"O=S(=O)(c1ccc(cc1)c3ccccc3(c2ccc(cc2)Cl))C" };
			//			String smarts4 = "O=S";// "c1ccccc1";
			//			MultiKabschAlignement.align("cox2", s4, smarts4);
			//			Settings.LOGGER.println();

			//			String s5[] = { "O=C2NC(=Nc1c2(ncn1COCCO))N", "O=C2NC(=Nc1c2(ncn1COC(CO)CO))N" };
			//			String smarts5 = "OC";
			//			MultiKabschAlignement.align("basic", s5, smarts5);
			//			Settings.LOGGER.println();
		}
		catch (Exception e)
		{
			Settings.LOGGER.error(e);
		}
	}

	private static void align(String name, String[] smiles, String smarts) throws IOException, CDKException,
			CloneNotSupportedException
	{
		SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
		IAtomContainer mol[] = new IAtomContainer[smiles.length];
		ModelBuilder3D mb3d = ModelBuilder3D.getInstance(TemplateHandler3D.getInstance(), "mm2",
				DefaultChemObjectBuilder.getInstance());
		for (int i = 0; i < mol.length; i++)
		{
			Settings.LOGGER.info("build molecule " + (i + 1) + "/" + mol.length);
			mol[i] = mb3d.generate3DCoordinates(sp.parseSmiles(smiles[i]), true);
		}
		toSDF(mol, "/tmp/" + name + ".before.sdf");
		MultiKabschAlignement.align(mol, smarts);
		toSDF(mol, "/tmp/" + name + ".after.sdf");
	}

	private static void toSDF(IAtomContainer mols[], String file) throws FileNotFoundException, IOException,
			CDKException
	{
		SDFWriter writer = new SDFWriter(new FileOutputStream(file));
		StructureDiagramGenerator sdg = new StructureDiagramGenerator();
		for (IAtomContainer mol : mols)
		{
			IAtomContainerSet oldSet = ConnectivityChecker.partitionIntoMolecules(mol);
			AtomContainer newSet = new AtomContainer();
			for (int i = 0; i < oldSet.getAtomContainerCount(); i++)
			{
				try
				{
					sdg.setMolecule(oldSet.getAtomContainer(i));
					sdg.generateCoordinates();
					newSet.add(AtomContainerManipulator.removeHydrogens(sdg.getMolecule()));
				}
				catch (Exception e)
				{
					Settings.LOGGER.error(e);
					newSet.add(AtomContainerManipulator.removeHydrogens(oldSet.getAtomContainer(i)));
				}
			}
			writer.write(newSet);
		}
		writer.close();
		Settings.LOGGER.info("written to " + file);
	}
}
