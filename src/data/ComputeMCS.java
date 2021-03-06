package data;

import java.io.File;
import java.util.List;

import main.Settings;
import main.TaskProvider;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.isomorphism.MCSComputer;
import org.openscience.cdk.smiles.SmilesGenerator;

import util.FileUtil;
import alg.align3d.ThreeDAligner;
import data.fragments.MatchEngine;
import dataInterface.ClusterData;
import dataInterface.SubstructureSmartsType;

public class ComputeMCS
{
	public static void computeMCS(DatasetFile dataset, List<ClusterData> clusters)
	{
		int count = 0;
		IMolecule allMols[] = dataset.getCompounds();

		for (ClusterData c : clusters)
		{
			String cacheFile = dataset.getAlignResultsPerClusterFilePath(count, "mcs");

			String smarts = null;
			if (Settings.CACHING_ENABLED && new File(cacheFile).exists())
			{
				Settings.LOGGER.info("Read cached mcs from: " + cacheFile);
				smarts = FileUtil.readStringFromFile(cacheFile);
				if (smarts.equals("null"))
					smarts = null;
			}
			else
			{
				if (MCSComputer.DEBUG)
					Settings.LOGGER.info("\n\n");

				TaskProvider.debug("Compute MCS for cluster " + count + "/" + clusters.size());

				IMolecule mols[] = new IMolecule[c.getNumCompounds()];
				for (int i = 0; i < mols.length; i++)
					mols[i] = allMols[c.getCompounds().get(i).getOrigIndex()];
				IAtomContainer mcsMolecule = null;
				try
				{
					mcsMolecule = MCSComputer.computeMCS(mols, ThreeDAligner.MIN_NUM_ATOMS);
				}
				catch (Exception e)
				{
					Settings.LOGGER.error(e);
				}
				if (mcsMolecule != null)
				{
					SmilesGenerator g = new SmilesGenerator(true, true);
					smarts = g.createSMILES(mcsMolecule);
					//HACK: otherwhise CDK cannot rematch the smarts 
					smarts = smarts.replaceAll("\\[nH\\]", "n");
					//				Settings.LOGGER.println("non aromatic");
					//				g = new SmilesGenerator();
					//				Settings.LOGGER.println(g.createSMILES(mcsMolecule));
				}
				if (MCSComputer.DEBUG)
					Settings.LOGGER.info("\n\n");
				FileUtil.writeStringToFile(cacheFile, smarts == null ? "null" : smarts);
			}
			if (smarts != null)
			{
				((ClusterDataImpl) c).setSubstructureSmarts(SubstructureSmartsType.MCS, smarts);
				((ClusterDataImpl) c).setSubstructureSmartsMatchEngine(SubstructureSmartsType.MCS, MatchEngine.CDK);
				Settings.LOGGER.info("MCSMolecule: " + c.getSubstructureSmarts(SubstructureSmartsType.MCS));
			}
			count++;
		}
	}
}
