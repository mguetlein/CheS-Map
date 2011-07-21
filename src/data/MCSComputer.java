package data;

import gui.Progressable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import main.Settings;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.smsd.AtomAtomMapping;
import org.openscience.smsd.Isomorphism;
import org.openscience.smsd.interfaces.Algorithm;

import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.SubstructureSmartsType;

public class MCSComputer
{
	private static Map<Integer, Integer> getIndexMapping(AtomAtomMapping aam)
	{
		Map<IAtom, IAtom> mappings = aam.getMappings();
		Map<Integer, Integer> mapping = new TreeMap<Integer, Integer>();
		for (IAtom keys : mappings.keySet())
		{
			mapping.put(aam.getQueryIndex(keys), aam.getTargetIndex(mappings.get(keys)));
		}
		return mapping;
	}

	private static IAtomContainer getSubgraph(IMolecule container, Map<Integer, Integer> mapping)
	{
		Collection<Integer> values = mapping.values();
		List<IAtom> subgraphAtoms = new ArrayList<IAtom>();
		IAtomContainer subgraph = null;
		try
		{
			subgraph = (IAtomContainer) container.clone();
		}
		catch (CloneNotSupportedException e)
		{
			e.printStackTrace();
		}
		for (Integer index : values)
		{
			subgraphAtoms.add(subgraph.getAtom(index));
		}
		List<IAtom> atoms = new ArrayList<IAtom>();
		for (IAtom atom : subgraph.atoms())
		{
			atoms.add(atom);
		}
		for (IAtom atom : atoms)
		{
			if (!subgraphAtoms.contains(atom))
			{
				subgraph.removeAtomAndConnectedElectronContainers(atom);
			}
		}
		return subgraph;
	}

	public static void computeMCS(DatasetFile dataset, List<ClusterData> clusters, Progressable progress)
	{
		int count = 0;
		for (ClusterData c : clusters)
		{
			progress.update(100 / (double) clusters.size() * count, "Computing MCS fo cluster " + (++count) + "/"
					+ clusters.size());

			IMolecule mols[] = dataset.getMolecules(false);
			IMolecule mcsMolecule = null;
			List<IAtomContainer> targets = new ArrayList<IAtomContainer>();
			boolean matchBonds = true;
			int filter = 0;

			for (CompoundData m : c.getCompounds())
			{
				IMolecule target = mols[m.getIndex()];
				boolean flag = ConnectivityChecker.isConnected(target);
				if (!flag)
				{
					System.out.println("WARNING : Skipping target molecule " + target.getProperty(CDKConstants.TITLE)
							+ " as it is not connected.");
					continue;
				}
				else
				{
					if (target.getProperty(CDKConstants.TITLE) != null)
					{
						target.setID((String) target.getProperty(CDKConstants.TITLE));
						//					argumentHandler.setTargetMolOutName(target.getID());
					}
				}
				//			if (removeHydrogens)
				//			{
				target = new Molecule(AtomContainerManipulator.removeHydrogens(target));
				//			}

				if (mcsMolecule != null)
				{
					flag = ConnectivityChecker.isConnected(mcsMolecule);
					if (!flag)
					{
						System.out.println("WARNING : Skipping file " + mcsMolecule.getProperty(CDKConstants.TITLE)
								+ " not connected ");
						return;

					}
					else if (mcsMolecule.getProperty(CDKConstants.TITLE) != null)
					{
						mcsMolecule.setID((String) mcsMolecule.getProperty(CDKConstants.TITLE));
						//					argumentHandler.setQueryMolOutName(mcsMolecule.getID());
					}
					//				if (removeHydrogens)
					//				{
					mcsMolecule = new Molecule(AtomContainerManipulator.removeHydrogens(mcsMolecule));
					//
					//				}
				}

				//			inputHandler.configure(target, targetType);

				if (mcsMolecule == null)
				{
					mcsMolecule = target;
					targets.add(target);
				}
				else
				{
					Isomorphism smsd = new Isomorphism(mcsMolecule, target, Algorithm.DEFAULT, matchBonds);
					if (filter == 0)
					{
						smsd.setChemFilters(false, false, false);
					}
					if (filter == 1)
					{
						smsd.setChemFilters(true, false, false);
					}
					if (filter == 2)
					{
						smsd.setChemFilters(true, true, false);
					}
					if (filter == 3)
					{
						smsd.setChemFilters(true, true, true);
					}

					target = target.getBuilder().newInstance(Molecule.class, smsd.getFirstAtomMapping().getTarget());
					targets.add(target);
					Map<Integer, Integer> mapping = getIndexMapping(smsd.getFirstAtomMapping());
					IAtomContainer subgraph = getSubgraph(target, mapping);
					mcsMolecule = new Molecule(subgraph);
				}

				if (Settings.isAborted(Thread.currentThread()))
					break;

			}
			if (mcsMolecule != null)
			{
				SmilesGenerator g = new SmilesGenerator();
				g.setUseAromaticityFlag(true);
				((ClusterDataImpl) c).setSubstructureSmarts(SubstructureSmartsType.MCS, g.createSMILES(mcsMolecule));
				System.out.println("MCSMolecule: " + c.getSubstructureSmarts(SubstructureSmartsType.MCS));
			}

			if (Settings.isAborted(Thread.currentThread()))
				break;
		}

	}
}
