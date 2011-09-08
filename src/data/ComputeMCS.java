package data;

import java.util.List;

import main.TaskProvider;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.isomorphism.MCSComputer;
import org.openscience.cdk.smiles.SmilesGenerator;

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
				mcsMolecule = MCSComputer.computeMCS(mols);
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
			if (MCSComputer.DEBUG)
				System.out.println("\n\n");
		}

	}

}
