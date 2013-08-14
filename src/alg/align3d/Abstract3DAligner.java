package alg.align3d;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Message;
import gui.Messages;
import io.ExternalTool.ExternalToolError;
import io.SDFUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

import main.BinHandler;
import main.Settings;
import main.TaskProvider;

import org.openscience.cdk.geometry.alignment.MultiKabschAlignement;
import org.openscience.cdk.interfaces.IMolecule;

import util.ExternalToolUtil;
import util.FileUtil;
import util.StringUtil;
import alg.AbstractAlgorithm;
import alg.cluster.DatasetClusterer;
import data.ClusterDataImpl;
import data.DatasetFile;
import data.FeatureService;
import data.fragments.MatchEngine;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.SubstructureSmartsType;

public abstract class Abstract3DAligner extends AbstractAlgorithm implements ThreeDAligner
{
	String alignedFiles[];

	@Override
	public String getAlginedClusterFile(int clusterIndex)
	{
		return alignedFiles[clusterIndex];
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		if (requiresStructuralFragments() && !featureInfo.smartsFeaturesSelected)
			m.add(Message.errorMessage(Settings.text("align.error.no-struct")));
		return m;
	}

	public abstract void giveNoSmartsWarning(int clusterIndex);

	public void alignToSmarts(DatasetFile dataset, List<ClusterData> clusters, SubstructureSmartsType type)
	{
		alignedFiles = new String[clusters.size()];

		int count = 0;
		for (ClusterData cluster : clusters)
		{
			boolean aligned = false;
			String destFile = null;

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
				destFile = Settings.destinationFile(
						dataset,
						FileUtil.getFilename(cluster.getFilename(), false) + "."
								+ StringUtil.getMD5(cluster.getSubstructureSmarts(type)) + "."
								+ cluster.getSubstructureSmartsMatchEngine(type) + ".aligned.sdf");

				if (Settings.CACHING_ENABLED && new File(destFile).exists())
				{
					Settings.LOGGER.info("3D aligned file already exists: " + destFile);
					aligned = true;
				}
				else
				{
					TaskProvider.update("Aligning cluster " + (count + 1) + "/" + clusters.size() + " according to "
							+ cluster.getSubstructureSmarts(type));
					if (cluster.getSubstructureSmartsMatchEngine(type) == MatchEngine.CDK)
						aligned = alignWithCDK(dataset, cluster, count, type, destFile);
					else if (cluster.getSubstructureSmartsMatchEngine(type) == MatchEngine.OpenBabel)
						aligned = alignWithOpenBabel(dataset, cluster, count, type, destFile);
					else
						throw new IllegalStateException();
				}
			}

			if (aligned)
			{
				((ClusterDataImpl) cluster).setAlignAlgorithm(getName());
				alignedFiles[count] = destFile;
			}
			else
			{
				((ClusterDataImpl) cluster).setAlignAlgorithm(NoAligner.getNameStatic());
				alignedFiles[count] = null;
			}
			count++;
		}
	}

	private boolean alignWithCDK(DatasetFile dataset, ClusterData cluster, int index, SubstructureSmartsType type,
			String destFile)
	{
		int compoundIndices[] = new int[cluster.getSize()];
		IMolecule compounds[] = new IMolecule[cluster.getSize()];
		String smarts = cluster.getSubstructureSmarts(type);

		for (int k = 0; k < cluster.getSize(); k++)
		{
			CompoundData comp = cluster.getCompounds().get(k);
			compoundIndices[k] = comp.getIndex();
			compounds[k] = dataset.getCompounds(false)[comp.getIndex()];
		}
		try
		{
			MultiKabschAlignement.align(compounds, smarts);
			FeatureService.writeCompoundsToSDFFile(dataset, destFile, compoundIndices, true);
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

	private boolean alignWithOpenBabel(DatasetFile dataset, ClusterData cluster, int index,
			SubstructureSmartsType type, String destFile)
	{
		String clusterFile = cluster.getFilename();
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
			// String alignedStructures = FileUtil.getParent(clusterFile) + File.separator
			// + FileUtil.getFilename(clusterFile, false) + ".aligned.sdf";

			SDFUtil.filter(clusterFile, tmpFirst.getAbsolutePath(), new int[] { 0 }, true);
			int remainderIndices[] = new int[cluster.getCompounds().size() - 1];
			for (int j = 0; j < remainderIndices.length; j++)
				remainderIndices[j] = (j + 1);
			SDFUtil.filter(clusterFile, tmpRemainder.getAbsolutePath(), remainderIndices, true);

			ExternalToolUtil.run("obfit", new String[] { BinHandler.BABEL_BINARY.getSisterCommandLocation("obfit"),
					cluster.getSubstructureSmarts(type), tmpFirst.getAbsolutePath(), tmpRemainder.getAbsolutePath() },
					tmpAligned);

			DatasetFile.clearFilesWith3DSDF(destFile);
			FileUtil.join(tmpFirst.getAbsolutePath(), tmpAligned.getAbsolutePath(), destFile);
			// if (SDFUtil.countCompounds(alignedStructures) != SDFUtil.countCompounds(clusterFile))
			// throw new IllegalStateException(alignedStructures + " "
			// + SDFUtil.countCompounds(alignedStructures) + " != " + clusterFile + " "
			// + SDFUtil.countCompounds(clusterFile));

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
