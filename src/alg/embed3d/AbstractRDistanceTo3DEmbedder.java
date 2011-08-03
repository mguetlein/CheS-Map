package alg.embed3d;

import gui.property.Property;
import io.FileResultCacher;
import io.RUtil;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import main.Settings;

import org.apache.commons.math.geometry.Vector3D;

import util.ArrayUtil;
import util.DistanceMatrix;
import util.ExternalToolUtil;
import util.FileUtil;
import data.DatasetFile;
import data.RScriptUser;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

public abstract class AbstractRDistanceTo3DEmbedder extends RScriptUser implements ThreeDEmbedder
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
			System.out.println("WARNING: " + getRScriptName() + " needs at least " + getMinNumInstances()
					+ " instances for embedding, returning 0-0-0 positions");
			random.embed(dataset, instances, features, distances);
			positions = random.positions;
			return;
		}

		String tableFile = Settings.destinationFile(dataset.getSDFPath(true),
				FileUtil.getFilename(dataset.getSDFPath(true), false) + "." + getRScriptName() + ".distances.table");
		String embeddingFile = Settings
				.destinationFile(dataset.getSDFPath(true), FileUtil.getFilename(dataset.getSDFPath(true), false) + "."
						+ getRScriptName() + ".distances.embedding");

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
				ExternalToolUtil.run(getRScriptName(), Settings.CV_RSCRIPT_PATH + " " + getScriptPath() + " " + infile
						+ " " + outfile);
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

	public abstract int getMinNumInstances();

	public static class SMACOF3DEmbedder extends AbstractRDistanceTo3DEmbedder
	{
		public int getMinNumInstances()
		{
			return 4;
		}

		@Override
		protected String getRScriptName()
		{
			return "smacof";
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

		@Override
		protected String getRScriptCode()
		{
			return "args <- commandArgs(TRUE)\n"
					+ //
					"\n"
					+ "library(\"smacof\")\n"
					+ "df = read.table(args[1])\n"
					+ "res <- smacofSym(df, ndim = 3, metric = FALSE, ties = \"secondary\", verbose = TRUE, itmax = 150)\n"
					+ "#res <- smacofSphere.dual(df, ndim = 3)\n" + "print(res$conf)\n" + "print(class(res$conf))\n"
					+ "write.table(res$conf,args[2]) ";
		}
	}

	public static class PCADistance3DEmbedder extends AbstractRDistanceTo3DEmbedder
	{
		public int getMinNumInstances()
		{
			return 3;
		}

		@Override
		protected String getRScriptName()
		{
			return "pca";
		}

		@Override
		public String getName()
		{
			return "PCA 3D-Embedder (using Distance)";
		}

		@Override
		public String getDescription()
		{
			return null;
		}

		@Override
		protected String getRScriptCode()
		{
			return "args <- commandArgs(TRUE)\n" //
					+ "df = read.table(args[1])\n" + "res <- princomp(df)\n"
					+ "print(res$scores[,1:3])\n"
					+ "write.table(res$scores[,1:3],args[2]) ";
		}
	}

	public static class TSNEDistance3DEmbedder extends AbstractRDistanceTo3DEmbedder
	{

		public int getMinNumInstances()
		{
			return 2;
		}

		@Override
		protected String getRScriptName()
		{
			return "tsne";
		}

		@Override
		public String getName()
		{
			return "TSNE 3D-Embedder (using Distances)";
		}

		@Override
		public String getDescription()
		{
			return null;
		}

		@Override
		protected String getRScriptCode()
		{
			return "args <- commandArgs(TRUE)\n" //
					+ "\n" + "library(\"tsne\")\n"
					+ "df = read.table(args[1])\n"
					+ "res <- tsne(df, k = 3, perplexity=150)\n" + "print(res$ydata)\n"
					+ "\n"
					+ "##res <- smacofSphere.dual(df, ndim = 3)\n" + "#print(res$conf)\n"
					+ "#print(class(res$conf))\n"
					+ "\n" + "write.table(res$ydata,args[2]) \n" + "";
		}
	}

}
