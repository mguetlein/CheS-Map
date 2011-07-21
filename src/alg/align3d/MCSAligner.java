package alg.align3d;

import gui.property.Property;
import io.ExternalTool;
import io.SDFUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import main.Settings;
import util.FileUtil;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.SubstructureSmartsType;

public class MCSAligner implements ThreeDAligner
{

	@Override
	public Property[] getProperties()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setProperties(Property[] properties)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public String getName()
	{
		return "MCS Aligner";
	}

	@Override
	public String getDescription()
	{
		return "Uses the obfit command from OpenBabel to align compounds of a cluster according to their Most Common Subgraph.";
	}

	List<String> alignedFiles;

	@Override
	public void algin(DatasetFile dataset, List<ClusterData> clusters)
	{
		alignedFiles = new ArrayList<String>();

		for (ClusterData cluster : clusters)
		{
			if (cluster.getCompounds().size() < 2)
				alignedFiles.add(cluster.getFilename());
			else
			{
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
					ExternalTool.run("obfit", tmpAligned, null,
							Settings.CV_OBFIT_PATH + " " + cluster.getSubstructureSmarts(SubstructureSmartsType.MCS)
									+ " " + tmpFirst.getAbsolutePath() + " " + tmpRemainder.getAbsolutePath()
					//						new String[] { "obfit", commonSubstructure[i], tmpFirst.getAbsolutePath(),
					//								tmpRemainder.getAbsolutePath(), ">", alignedStructures }
							);

					DatasetFile.clearFilesWithSDF(alignedStructures);

					FileUtil.join(tmpFirst.getAbsolutePath(), tmpAligned.getAbsolutePath(), alignedStructures);
					alignedFiles.add(alignedStructures);
					//					if (SDFUtil.countCompounds(alignedStructures) != SDFUtil.countCompounds(clusterFile))
					//						throw new IllegalStateException(alignedStructures + " "
					//								+ SDFUtil.countCompounds(alignedStructures) + " != " + clusterFile + " "
					//								+ SDFUtil.countCompounds(clusterFile));
				}
				catch (Error e)
				{
					System.err.println("ERROR in algin: " + e.getMessage());
					alignedFiles.add(cluster.getFilename());
				}
			}
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
		ExternalTool
				.run("obfit",
						null,
						null,
						"obfit Oc1ccc(cc1)-c1cocc(:c:c)c1=O /tmp/first4154035072070520801sdf /tmp/remainder4312806650036993699sdf > /tmp/structural_cluster_3.aligned.sdf");
	}

	@Override
	public boolean isRealAligner()
	{
		return true;
	}

	@Override
	public String getPreconditionErrors()
	{
		if (Settings.CV_OBFIT_PATH != null)
			return null;
		else
			return "OpenBabel command 'obfit' could not be found";
	}

}
