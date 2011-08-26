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
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.nonotify.NoNotificationChemObjectBuilder;
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

	private static Isomorphism run(IMolecule mcsMolecule, IMolecule target, int filter, boolean matchBonds)
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
		return smsd;
	}

	public static void computeMCS(DatasetFile dataset, List<ClusterData> clusters, Progressable progress)
	{
		int count = 0;
		boolean matchBonds = true;
		boolean removeHydrogens = true;
		int filter = 0;

		IMolecule mols[] = dataset.getMolecules(false);

		for (ClusterData c : clusters)
		{
			progress.update(100 / (double) clusters.size() * count, "Computing MCS for cluster " + (++count) + "/"
					+ clusters.size());
			IMolecule mcsMolecule = null;
			try
			{
				List<IAtomContainer> targets = new ArrayList<IAtomContainer>();
				for (CompoundData m : c.getCompounds())
				{
					IMolecule target = mols[m.getIndex()];

					boolean flag = ConnectivityChecker.isConnected(target);
					if (!flag)
					{
						System.out.println("WARNING : Skipping target molecule "
								+ target.getProperty(CDKConstants.TITLE) + " as it is not connected.");
						continue;
					}
					else
					{
						if (target.getProperty(CDKConstants.TITLE) != null)
						{
							target.setID((String) target.getProperty(CDKConstants.TITLE));
							//						argumentHandler.setTargetMolOutName(target.getID());
						}
					}
					if (removeHydrogens)
					{
						target = new Molecule(AtomContainerManipulator.removeHydrogens(target));
					}

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
							//						argumentHandler.setQueryMolOutName(mcsMolecule.getID());
						}
						if (removeHydrogens)
						{
							mcsMolecule = new Molecule(AtomContainerManipulator.removeHydrogens(mcsMolecule));

						}
					}

					//				inputHandler.configure(target, targetType);

					if (mcsMolecule == null)
					{
						mcsMolecule = target;
						targets.add(target);
					}
					else
					{
						Isomorphism smsd = run(mcsMolecule, target, filter, matchBonds);
						target = target.getBuilder().newInstance(IMolecule.class,
								smsd.getFirstAtomMapping().getTarget());
						targets.add(target);
						Map<Integer, Integer> mapping = getIndexMapping(smsd.getFirstAtomMapping());
						IAtomContainer subgraph = getSubgraph(target, mapping);
						mcsMolecule = new Molecule(subgraph);
					}
					if (Settings.isAborted(Thread.currentThread()))
						break;
				}
				if (Settings.isAborted(Thread.currentThread()))
					break;
				//			inputHandler.configure(mcsMolecule, targetType);
				//			if (argumentHandler.shouldOutputSubgraph())
				//			{
				//				String outpath = argumentHandler.getOutputFilepath();
				//				String outtype = argumentHandler.getOutputFiletype();
				//				outputHandler.writeMol(outtype, mcsMolecule, outpath);
				//			}
				if (mcsMolecule != null)// && argumentHandler.isImage())
				{
					// now that we have the N-MCS, remap
					List<Map<Integer, Integer>> mappings = new ArrayList<Map<Integer, Integer>>();
					List<IAtomContainer> secondRoundTargets = new ArrayList<IAtomContainer>();
					IChemObjectBuilder builder = NoNotificationChemObjectBuilder.getInstance();
					for (IAtomContainer target : targets)
					{
						Isomorphism smsd = run(mcsMolecule, (IMolecule) target, filter, matchBonds);
						mappings.add(getIndexMapping(smsd.getFirstAtomMapping()));
						secondRoundTargets.add(builder.newInstance(IAtomContainer.class, smsd.getFirstAtomMapping()
								.getTarget()));
						if (Settings.isAborted(Thread.currentThread()))
							break;
					}

					//				String name = inputHandler.getTargetName();
					//				outputHandler.writeCircleImage(mcsMolecule, secondRoundTargets, name, mappings);

				}
				if (Settings.isAborted(Thread.currentThread()))
					break;
			}
			catch (Exception e)
			{
				System.err.println("Error in MCS computation: " + e.getMessage());
				e.printStackTrace();
			}
			if (mcsMolecule != null)
			{
				SmilesGenerator g = new SmilesGenerator();
				g.setUseAromaticityFlag(true);
				((ClusterDataImpl) c).setSubstructureSmarts(SubstructureSmartsType.MCS, g.createSMILES(mcsMolecule));
				System.out.println("MCSMolecule: " + c.getSubstructureSmarts(SubstructureSmartsType.MCS));
			}
		}

	}

}
