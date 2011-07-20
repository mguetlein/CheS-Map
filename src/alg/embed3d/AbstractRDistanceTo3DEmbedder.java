package alg.embed3d;

import gui.property.Property;
import io.ExternalTool;
import io.FileResultCacher;
import io.RUtil;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import main.Settings;

import org.apache.commons.math.geometry.Vector3D;

import util.ArrayUtil;
import util.DistanceMatrix;
import util.FileUtil;
import data.DatasetFile;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

public abstract class AbstractRDistanceTo3DEmbedder implements ThreeDEmbedder
{
	List<Vector3f> positions;

	Random3DEmbedder random = new Random3DEmbedder();

	@Override
	public boolean requiresNumericalFeatures()
	{
		return true;
	}

	@Override
	public void embed(DatasetFile dataset, List<MolecularPropertyOwner> instances, List<MoleculeProperty> features,
			final DistanceMatrix<MolecularPropertyOwner> distances)
	{
		//		String clusterNames[] = new String[clusterFiles.length];
		//		for (int i = 0; i < clusterNames.length; i++)
		//			clusterNames[i] = clusterFiles[i].substring(clusterFiles[i].lastIndexOf(File.separator) + 1);

		if (instances.size() < getMinNumInstances())
		{
			System.out.println("WARNING: " + getInitials() + " needs at least " + getMinNumInstances()
					+ " instances for embedding, returning 0-0-0 positions");
			random.embed(dataset, instances, features, distances);
			positions = random.positions;
			return;
		}

		String tableFile = Settings.destinationFile(dataset.getSDFPath(),
				FileUtil.getFilename(dataset.getSDFPath(), false) + "." + getInitials() + ".distances.table");
		String embeddingFile = Settings.destinationFile(dataset.getSDFPath(),
				FileUtil.getFilename(dataset.getSDFPath(), false) + "." + getInitials() + ".distances.embedding");

		FileResultCacher.InFileWriter inWriter = new FileResultCacher.InFileWriter()
		{
			@Override
			public void write(String filename)
			{
				RUtil.toRMatrixTable(distances, filename);
			}
		};
		FileResultCacher.OutFileWriter outWriter = new FileResultCacher.OutFileWriter()
		{
			@Override
			public void write(String infile, String outfile)
			{
				ExternalTool.run(getRScript(), null, null, Settings.CV_RSCRIPT_PATH + " /home/martin/software/R/"
						+ getRScript() + " " + infile + " " + outfile);
			}
		};
		FileResultCacher frc = new FileResultCacher(tableFile, embeddingFile, inWriter, outWriter);
		tableFile = frc.getInfile();
		embeddingFile = frc.getOufile();

		List<Vector3D> v3d = RUtil.readRVectorMatrix(embeddingFile);

		double d[][] = new double[v3d.size()][3];
		for (int i = 0; i < v3d.size(); i++)
		{
			d[i][0] = (float) v3d.get(i).getX();
			d[i][1] = (float) v3d.get(i).getY();
			d[i][2] = (float) v3d.get(i).getZ();
		}

		//		System.out.println("before: " + ArrayUtil.toString(d));
		ArrayUtil.normalize(d, -1, 1);
		if (Settings.DBG)
			System.out.println("normalized positions: " + ArrayUtil.toString(d));

		positions = new ArrayList<Vector3f>();
		for (int i = 0; i < v3d.size(); i++)
			positions.add(new Vector3f((float) d[i][0], (float) d[i][1], (float) d[i][2]));
		//v3f[i] = new Vector3f((float) v3d.get(i).getX(), (float) v3d.get(i).getY(), (float) v3d.get(i).getZ());
	}

	@Override
	public String getPreconditionErrors()
	{
		if (Settings.CV_RSCRIPT_PATH == null)
			return "R command 'Rscript' could not be found";
		else
			return null;
	}

	@Override
	public List<Vector3f> getPositions()
	{
		return positions;
	}

	@Override
	public Property[] getProperties()
	{
		return new Property[0];
	}

	@Override
	public void setProperties(Property[] properties)
	{

	}

	@Override
	public boolean requiresDistances()
	{
		return true;
	}

	public abstract String getRScript();

	public abstract String getInitials();

	public abstract int getMinNumInstances();

	public static class SMACOF3DEmbedder extends AbstractRDistanceTo3DEmbedder
	{
		public int getMinNumInstances()
		{
			return 4;
		}

		public String getInitials()
		{
			return "smacof";
		}

		@Override
		public String getRScript()
		{
			return "smacof.R";
		}

		@Override
		public String getName()
		{
			return "SMACOF 3D-Embedder (using Distance)";
		}

		@Override
		public String getDescription()
		{
			// TODO Auto-generated method stub
			return null;
		}
	}

	public static class PCADistance3DEmbedder extends AbstractRDistanceTo3DEmbedder
	{
		public int getMinNumInstances()
		{
			return 3;
		}

		public String getInitials()
		{
			return "pca";
		}

		@Override
		public String getRScript()
		{
			return "pca.R";
		}

		@Override
		public String getName()
		{
			return "PCA 3D-Embedder (using Distance)";
		}

		@Override
		public String getDescription()
		{
			// TODO Auto-generated method stub
			return null;
		}
	}

	public static class TSNEDistance3DEmbedder extends AbstractRDistanceTo3DEmbedder
	{

		public int getMinNumInstances()
		{
			return 2;
		}

		public String getInitials()
		{
			return "tsne";
		}

		@Override
		public String getRScript()
		{
			return "tsne.R";
		}

		@Override
		public String getName()
		{
			return "SMACOF 3D-Embedder (using Distance)";
		}

		@Override
		public String getDescription()
		{
			return null;
		}
	}

}
