package org.chesmapper.map.data;

import java.io.File;
import java.util.List;

import org.chesmapper.map.alg.align3d.ThreeDAligner;
import org.chesmapper.map.data.fragments.MatchEngine;
import org.chesmapper.map.dataInterface.ClusterData;
import org.chesmapper.map.dataInterface.SubstructureSmartsType;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.mg.javalib.util.FileUtil;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.MCSComputer;
import org.openscience.cdk.isomorphism.MCSComputer.SMARTSGenerator;

public class ComputeMCS
{
	public static void computeMCS(DatasetFile dataset, List<ClusterData> clusters)
	{
		int count = 0;
		IAtomContainer allMols[] = dataset.getCompounds();

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

				IAtomContainer mols[] = new IAtomContainer[c.getNumCompounds()];
				for (int i = 0; i < mols.length; i++)
					mols[i] = allMols[c.getCompounds().get(i).getOrigIndex()];
				IAtomContainer mcsMolecule = null;
				try
				{
					mcsMolecule = MCSComputer.computeMCS(mols, ThreeDAligner.MIN_NUM_ATOMS);
					smarts = SMARTSGenerator.create(mcsMolecule);

					//					if (mcsMolecule != null)
					//					{
					//						SmilesGenerator g = SmilesGenerator.generic();
					//						//					SmilesGenerator g = new SmilesGenerator(true, true);
					//						smarts = g.create(mcsMolecule);
					//						//HACK: otherwhise CDK cannot rematch the smarts 
					//						smarts = smarts.replaceAll("\\[nH\\]", "n");
					//						//				Settings.LOGGER.println("non aromatic");
					//						//				g = new SmilesGenerator();
					//						//				Settings.LOGGER.println(g.createSMILES(mcsMolecule));
					//					}
				}
				catch (Exception e)
				{
					Settings.LOGGER.error(e);
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
