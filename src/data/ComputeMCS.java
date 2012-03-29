package data;

import java.util.List;

import main.TaskProvider;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.isomorphism.MCSComputer;
import org.openscience.cdk.smiles.SmilesGenerator;

import alg.align3d.ThreeDAligner;
import data.fragments.MatchEngine;
import dataInterface.ClusterData;
import dataInterface.SubstructureSmartsType;

public class ComputeMCS
{
	public static void computeMCS(DatasetFile dataset, List<ClusterData> clusters)
	{
		int count = 0;

		IMolecule allMols[] = dataset.getMolecules(false);

		for (ClusterData c : clusters)
		{
			if (MCSComputer.DEBUG)
				System.out.println("\n\n");

			TaskProvider.task().update("Computing MCS for cluster " + (++count) + "/" + clusters.size());

			IMolecule mols[] = new IMolecule[c.getSize()];
			for (int i = 0; i < mols.length; i++)
				mols[i] = allMols[c.getCompounds().get(i).getIndex()];
			IAtomContainer mcsMolecule = null;
			try
			{
				mcsMolecule = MCSComputer.computeMCS(mols, ThreeDAligner.MIN_NUM_ATOMS);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			if (mcsMolecule != null)
			{
				SmilesGenerator g = new SmilesGenerator(true, true);
				String smiles = g.createSMILES(mcsMolecule);
				//HACK: otherwhise CDK cannot rematch the smarts 
				smiles = smiles.replaceAll("\\[nH\\]", "n");
				((ClusterDataImpl) c).setSubstructureSmarts(SubstructureSmartsType.MCS, smiles);
				((ClusterDataImpl) c).setSubstructureSmartsMatchEngine(SubstructureSmartsType.MCS, MatchEngine.CDK);

				System.out.println("MCSMolecule: " + c.getSubstructureSmarts(SubstructureSmartsType.MCS));

				//				System.out.println("non aromatic");
				//				g = new SmilesGenerator();
				//				System.out.println(g.createSMILES(mcsMolecule));
			}
			if (MCSComputer.DEBUG)
				System.out.println("\n\n");
		}

	}
}
