package alg.align3d;

import gui.binloc.Binary;
import gui.property.Property;
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

public class MCSAligner implements ThreeDAligner
{

	@Override
	public Property[] getProperties()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return "Maximum Common Subgraph (MCS) Aligner";
	}

	@Override
	public String getDescription()
	{
		return "First, the Maximum Common Subgraph (MCS) of each cluster is computed. This is hard to do and will take long for large clusters (runtime is O(n²)).\n"
				+ "This is done with "
				+ Settings.CDK_STRING
				+ ".\n\n"
				+ "Second the compounds of each cluster are aligned according to their MCS. "
				+ "Hence, their orientation in 3D space is adjusted such that the common substructure is superimposed.\n"
				+ "This is done with " + Settings.OPENBABEL_STRING + ", using the obfit command.";
	}

	List<String> alignedFiles;

	@Override
	public void algin(DatasetFile dataset, List<ClusterData> clusters)
	{
		alignedFiles = new ArrayList<String>();

		int count = 0;
		for (ClusterData cluster : clusters)
		{
			if (cluster.getCompounds().size() < 2 || cluster.getSubstructureSmarts(SubstructureSmartsType.MCS) == null
					|| cluster.getSubstructureSmarts(SubstructureSmartsType.MCS).trim().length() == 0)
				alignedFiles.add(cluster.getFilename());
			else
			{
				TaskProvider.task().update(
						"Aligning cluster " + (count + 1) + "/" + clusters.size() + " according to "
								+ cluster.getSubstructureSmarts(SubstructureSmartsType.MCS));

				String clusterFile = cluster.getFilename();
				File tmpFirst = null;
				File tmpRemainder = null;
				File tmpAligned = null;
				try
				{
					tmpFirst = File.createTempFile("first.", ".sdf");
					tmpRemainder = File.createTempFile("remainder.", ".sdf");
					tmpAligned = File.createTempFile("aligned.", ".sdf");
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				//				String alignedStructures = FileUtil.getParent(clusterFile) + File.separator
				//						+ FileUtil.getFilename(clusterFile, false) + ".aligned.sdf";
				String alignedStructures = Settings.destinationFile(clusterFile,
						FileUtil.getFilename(clusterFile, false) + ".aligned.sdf");

				SDFUtil.filter(clusterFile, tmpFirst.getAbsolutePath(), new int[] { 0 });
				int remainderIndices[] = new int[cluster.getCompounds().size() - 1];
				for (int j = 0; j < remainderIndices.length; j++)
					remainderIndices[j] = (j + 1);
				SDFUtil.filter(clusterFile, tmpRemainder.getAbsolutePath(), remainderIndices);
				try
				{
					ExternalToolUtil.run(
							"obfit",
							Settings.BABEL_BINARY.getSisterCommandLocation("obfit") + " "
									+ cluster.getSubstructureSmarts(SubstructureSmartsType.MCS) + " "
									+ tmpFirst.getAbsolutePath() + " " + tmpRemainder.getAbsolutePath(), tmpAligned);

					DatasetFile.clearFilesWith3DSDF(alignedStructures);

					FileUtil.join(tmpFirst.getAbsolutePath(), tmpAligned.getAbsolutePath(), alignedStructures);
					alignedFiles.add(alignedStructures);
					//					if (SDFUtil.countCompounds(alignedStructures) != SDFUtil.countCompounds(clusterFile))
					//						throw new IllegalStateException(alignedStructures + " "
					//								+ SDFUtil.countCompounds(alignedStructures) + " != " + clusterFile + " "
					//								+ SDFUtil.countCompounds(clusterFile));
					((ClusterDataImpl) cluster).setAligned(true);
					((ClusterDataImpl) cluster).setAlignAlgorithm(getName());
				}
				catch (Error e)
				{
					System.err.println("ERROR in algin: " + e.getMessage());
					TaskProvider.task().warning(
							"Cannot align cluster " + (count + 1) + " (size: " + cluster.getSize() + ") according to "
									+ cluster.getSubstructureSmarts(SubstructureSmartsType.MCS), e);
					alignedFiles.add(cluster.getFilename());
					((ClusterDataImpl) cluster).setAlignAlgorithm("Failed: " + getName());
				}
			}

			count++;
		}
	}

	@Override
	public List<String> getAlginedClusterFiles()
	{
		return alignedFiles;
	}

	//	private static String escaptSmiles(String smiles)
	//	{
	//		String s = smiles.replace("(", "\\(");
	//		s = s.replace(")", "\\)");
	//		return s;
	//	}

	public static void main(String args[])
	{
		ExternalToolUtil
				.run("obfit",
						"obfit Oc1ccc(cc1)-c1cocc(:c:c)c1=O /tmp/first4154035072070520801sdf /tmp/remainder4312806650036993699sdf > /tmp/structural_cluster_3.aligned.sdf");
	}

	@Override
	public Binary getBinary()
	{
		return Settings.BABEL_BINARY;
	}

	@Override
	public String getWarning()
	{
		return null;
	}

}
