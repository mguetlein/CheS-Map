package alg.align3d;

import gui.binloc.Binary;
import io.ExternalTool.ExternalToolError;
import io.SDFUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import main.Settings;
import main.TaskProvider;
import util.ExternalToolUtil;
import util.FileUtil;
import data.ClusterDataImpl;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.SubstructureSmartsType;

public abstract class OBFitAligner extends Abstract3DAligner
{

	@Override
	public Binary getBinary()
	{
		return Settings.BABEL_BINARY;
	}

	List<String> alignedFiles;

	@Override
	public List<String> getAlginedClusterFiles()
	{
		return alignedFiles;
	}

	public void algin(DatasetFile dataset, List<ClusterData> clusters, SubstructureSmartsType type)
	{
		alignedFiles = new ArrayList<String>();

		int count = 0;
		for (ClusterData cluster : clusters)
		{
			if (cluster.getCompounds().size() < 2 || cluster.getSubstructureSmarts(type) == null
					|| cluster.getSubstructureSmarts(type).trim().length() == 0)
				alignedFiles.add(cluster.getFilename());
			else
			{
				TaskProvider.task().update(
						"Aligning cluster " + (count + 1) + "/" + clusters.size() + " according to "
								+ cluster.getSubstructureSmarts(type));

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
					String alignedStructures = Settings.destinationFile(dataset,
							FileUtil.getFilename(clusterFile, false) + ".aligned.sdf");

					SDFUtil.filter(clusterFile, tmpFirst.getAbsolutePath(), new int[] { 0 });
					int remainderIndices[] = new int[cluster.getCompounds().size() - 1];
					for (int j = 0; j < remainderIndices.length; j++)
						remainderIndices[j] = (j + 1);
					SDFUtil.filter(clusterFile, tmpRemainder.getAbsolutePath(), remainderIndices);

					ExternalToolUtil.run(
							"obfit",
							new String[] { Settings.BABEL_BINARY.getSisterCommandLocation("obfit"),
									cluster.getSubstructureSmarts(type), tmpFirst.getAbsolutePath(),
									tmpRemainder.getAbsolutePath() }, tmpAligned);

					DatasetFile.clearFilesWith3DSDF(alignedStructures);

					FileUtil.join(tmpFirst.getAbsolutePath(), tmpAligned.getAbsolutePath(), alignedStructures);
					// if (SDFUtil.countCompounds(alignedStructures) != SDFUtil.countCompounds(clusterFile))
					// throw new IllegalStateException(alignedStructures + " "
					// + SDFUtil.countCompounds(alignedStructures) + " != " + clusterFile + " "
					// + SDFUtil.countCompounds(clusterFile));
					alignedFiles.add(alignedStructures);
					((ClusterDataImpl) cluster).setAligned(true);
					((ClusterDataImpl) cluster).setAlignAlgorithm(getName());
				}
				catch (ExternalToolError e)
				{
					e.printStackTrace();
					TaskProvider
							.task()
							.warning(
									"obfit failed on aligning cluster " + (count + 1),
									e.getMessage()
											+ "\nMost likely the error cause is that CDK and OpenBabel have different aromaticity definitions");

					alignedFiles.add(null);
					e.printStackTrace();
					System.err.println("error occured, checking if smarts are matching in openbabel for debugging");
					ExternalToolUtil.run("match-ref",
							new String[] { Settings.BABEL_BINARY.getLocation(), tmpFirst.getAbsolutePath(), "-osmi",
									"--filter", "\"s='" + cluster.getSubstructureSmarts(type) + "'\"" });
					ExternalToolUtil.run("match-mve",
							new String[] { Settings.BABEL_BINARY.getLocation(), tmpRemainder.getAbsolutePath(),
									"-osmi", "--filter", "\"s='" + cluster.getSubstructureSmarts(type) + "'\"" });
					//						//convert error to runtime-exception, to not abort the mapping
					//						throw new RuntimeException(e.getMessage(), e.getCause());
				}
				finally
				{
					tmpFirst.delete();
					tmpRemainder.delete();
					tmpAligned.delete();
				}
			}

			count++;
		}
	}
}
