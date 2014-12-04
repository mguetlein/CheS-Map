package org.chesmapper.map.alg.align3d;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.chesmapper.map.alg.AbstractAlgorithm;
import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.data.ClusterDataImpl;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.FeatureService;
import org.chesmapper.map.data.fragments.MatchEngine;
import org.chesmapper.map.dataInterface.ClusterData;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.SubstructureSmartsType;
import org.chesmapper.map.gui.FeatureWizardPanel.FeatureInfo;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.chesmapper.map.util.ExternalToolUtil;
import org.mg.javalib.gui.Message;
import org.mg.javalib.gui.Messages;
import org.mg.javalib.io.SDFUtil;
import org.mg.javalib.io.ExternalTool.ExternalToolError;
import org.mg.javalib.util.FileUtil;
import org.openscience.cdk.geometry.alignment.MultiKabschAlignement;
import org.openscience.cdk.interfaces.IAtomContainer;

public abstract class Abstract3DAligner extends AbstractAlgorithm implements ThreeDAligner
{
	String alignedFile;

	@Override
	public String getAlignedSDFile()
	{
		return alignedFile;
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		if (Settings.BIG_DATA)
			return Messages.warningMessage(Settings.text("align.warn.ignored-because-big-data"));
		{
			Messages m = super.getMessages(dataset, featureInfo, clusterer);
			if (requiresStructuralFragments() && !featureInfo.isFragmentFeatureSelected())
				m.add(Message.errorMessage(Settings.text("align.error.no-struct")));
			return m;
		}
	}

	public abstract void giveNoSmartsWarning(int clusterIndex);

	public void alignToSmarts(DatasetFile dataset, List<ClusterData> clusters)
	{
		SubstructureSmartsType type = getSubstructureSmartsType();
		alignedFile = dataset.getAlignSDFilePath();
		if (Settings.CACHING_ENABLED && new File(alignedFile).exists())
		{
			Settings.LOGGER.info("3D aligned file already exists: " + alignedFile);

			int count = 0;
			for (ClusterData cluster : clusters)
			{
				MatchEngine matchEngine = cluster.getSubstructureSmartsMatchEngine(type);
				String destFile = dataset.getAlignResultsPerClusterFilePath(count, cluster.getSubstructureSmarts(type)
						+ matchEngine);
				boolean aligned = new File(destFile).exists();
				((ClusterDataImpl) cluster).setAlignAlgorithm(getName(), aligned);
				count++;
			}
		}
		else
		{
			String alignedFiles[] = new String[clusters.size()];
			int count = 0;
			for (ClusterData cluster : clusters)
			{
				boolean aligned = false;
				String destFile = null;
				MatchEngine matchEngine = cluster.getSubstructureSmartsMatchEngine(type);

				if (cluster.getCompounds().size() < 2)
				{
					// dont give a warning, single compound clusters cannot be aligned
				}
				else if (cluster.getSubstructureSmarts(type) == null
						|| cluster.getSubstructureSmarts(type).trim().length() == 0)
				{
					giveNoSmartsWarning(count);
				}
				else
				{
					destFile = dataset.getAlignResultsPerClusterFilePath(count, cluster.getSubstructureSmarts(type)
							+ matchEngine);

					if (Settings.CACHING_ENABLED && new File(destFile).exists())
					{
						Settings.LOGGER.info("3D aligned file already exists: " + destFile);
						aligned = true;
					}
					else
					{
						TaskProvider.update("3D align cluster " + (count + 1) + "/" + clusters.size()
								+ " according to " + cluster.getSubstructureSmarts(type));
						if (matchEngine == MatchEngine.CDK)
							aligned = alignWithCDK(dataset, cluster, count, destFile);
						else if (matchEngine == MatchEngine.OpenBabel)
							aligned = alignWithOpenBabel(dataset, cluster, count, destFile);
						else
							throw new IllegalStateException();
					}
				}

				if (aligned)
				{
					((ClusterDataImpl) cluster).setAlignAlgorithm(getName(), true);
					alignedFiles[count] = destFile;
				}
				else
				{
					((ClusterDataImpl) cluster).setAlignAlgorithm(NoAligner.getNameStatic(), false);
					alignedFiles[count] = null;
				}
				count++;
			}

			Settings.LOGGER.info("Create 3D aligend file: " + alignedFile);
			FileUtil.copy(dataset.getSDFClustered(), alignedFile);
			int i = 0;
			for (ClusterData cluster : clusters)
			{
				if (alignedFiles[i] == null)
				{
					if (cluster.isAligned())
						throw new IllegalStateException();
					//do nothing;
				}
				else
				{
					if (!cluster.isAligned())
						throw new IllegalStateException();
					SDFUtil.replaceCompounds(alignedFile, alignedFiles[i], cluster.getCompoundClusterIndices());
				}
				i++;
			}
		}
	}

	private boolean alignWithCDK(DatasetFile dataset, ClusterData cluster, int index, String destFile)
	{
		int compoundOrigIndices[] = new int[cluster.getNumCompounds()];
		IAtomContainer compounds[] = new IAtomContainer[cluster.getNumCompounds()];
		String smarts = cluster.getSubstructureSmarts(getSubstructureSmartsType());

		for (int k = 0; k < cluster.getNumCompounds(); k++)
		{
			CompoundData comp = cluster.getCompounds().get(k);
			compoundOrigIndices[k] = comp.getOrigIndex();
			compounds[k] = dataset.getCompounds()[comp.getOrigIndex()];
		}
		try
		{
			MultiKabschAlignement.align(compounds, smarts);
			FeatureService.writeOrigCompoundsToSDFile(dataset.getCompounds(), destFile, compoundOrigIndices, true,
					false);
			return true;
		}
		catch (Exception e)
		{
			Settings.LOGGER.error(e);
			TaskProvider.warning(getName() + " failed on cluster " + (index + 1), e.getMessage());
			if (new File(destFile).exists())
				new File(destFile).delete();
			return false;
		}
	}

	private boolean alignWithOpenBabel(DatasetFile dataset, ClusterData cluster, int index, String destFile)
	{
		SubstructureSmartsType type = getSubstructureSmartsType();

		//		String clusterFile = cluster.getFilename();
		File tmpFirst = null;
		File tmpRemainder = null;
		File tmpAligned = null;
		try
		{
			try
			{
				tmpFirst = File.createTempFile("first.", ".sdf");
				tmpRemainder = File.createTempFile("remainder.", ".sdf");
				tmpAligned = File.createTempFile("aligned.", ".sdf");
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}

			SDFUtil.filter(dataset.getSDFClustered(), tmpFirst.getAbsolutePath(), new int[] { cluster
					.getCompoundClusterIndices().get(0) }, true);
			int remainderIndices[] = new int[cluster.getCompounds().size() - 1];
			for (int j = 0; j < remainderIndices.length; j++)
				remainderIndices[j] = cluster.getCompoundClusterIndices().get(j + 1);
			SDFUtil.filter(dataset.getSDFClustered(), tmpRemainder.getAbsolutePath(), remainderIndices, true);

			ExternalToolUtil.run("obfit", new String[] { BinHandler.BABEL_BINARY.getSisterCommandLocation("obfit"),
					cluster.getSubstructureSmarts(type), tmpFirst.getAbsolutePath(), tmpRemainder.getAbsolutePath() },
					tmpAligned);

			FileUtil.join(tmpFirst.getAbsolutePath(), tmpAligned.getAbsolutePath(), destFile);

			return true;
		}
		catch (ExternalToolError e)
		{
			Settings.LOGGER.error(e);
			TaskProvider.warning("obfit failed on aligning cluster " + (index + 1), e.getMessage()
					+ "\nMost likely the error cause is that CDK and OpenBabel have different aromaticity definitions");
			Settings.LOGGER.warn("error occured, checking if smarts are matching in openbabel for debugging");
			ExternalToolUtil.run("match-ref",
					new String[] { BinHandler.BABEL_BINARY.getLocation(), tmpFirst.getAbsolutePath(), "-osmi",
							"--filter", "\"s='" + cluster.getSubstructureSmarts(type) + "'\"" });
			ExternalToolUtil.run("match-mve",
					new String[] { BinHandler.BABEL_BINARY.getLocation(), tmpRemainder.getAbsolutePath(), "-osmi",
							"--filter", "\"s='" + cluster.getSubstructureSmarts(type) + "'\"" });
			//						//convert error to runtime-exception, to not abort the mapping
			//						throw new RuntimeException(e.getMessage(), e.getCause());
			if (new File(destFile).exists())
				new File(destFile).delete();
			return false;
		}
		finally
		{
			tmpFirst.delete();
			tmpRemainder.delete();
			tmpAligned.delete();
		}
	}
}
