package alg.embed3d;

import gui.binloc.Binary;
import gui.property.IntegerProperty;
import gui.property.Property;
import io.RUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import main.Settings;

import org.apache.commons.math.geometry.Vector3D;

import rscript.ExportRUtil;
import rscript.RScriptUtil;
import util.ArrayUtil;
import util.ExternalToolUtil;
import data.DatasetFile;
import data.DistanceUtil;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

public abstract class AbstractRTo3DEmbedder extends Abstract3DEmbedder
{
	protected int numInstances = -1;

	protected abstract String getRScriptName();

	protected abstract String getRScriptCode();

	@Override
	public void embed(DatasetFile dataset, List<MolecularPropertyOwner> instances, List<MoleculeProperty> features)
	{
		this.numInstances = instances.size();

		if (features.size() < getMinNumFeatures() || instances.size() < getMinNumInstances())
			throw new Error(getRScriptName() + " needs at least " + getMinNumFeatures() + " features and "
					+ getMinNumInstances() + " instances for embedding");

		File f = null;
		File f2 = null;
		try
		{
			f = File.createTempFile("features", ".table");
			f2 = File.createTempFile("embedding", ".matrix");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		ExportRUtil.toRTable(features, DistanceUtil.values(features, instances), f.getAbsolutePath());

		ExternalToolUtil.run(
				getRScriptName(),
				Settings.RSCRIPT_BINARY.getLocation() + " "
						+ RScriptUtil.getScriptPath(getRScriptName(), getRScriptCode()) + " " + f.getAbsolutePath()
						+ " " + f2.getAbsolutePath());

		List<Vector3D> v3d = RUtil.readRVectorMatrix(f2.getAbsolutePath());
		if (v3d.size() != instances.size())
			throw new IllegalStateException("error using '" + getRScriptName() + "' num results is '" + v3d.size()
					+ "' instead of '" + instances.size() + "'");

		boolean nonZero = false;
		double d[][] = new double[v3d.size()][3];
		for (int i = 0; i < v3d.size(); i++)
		{
			d[i][0] = (float) v3d.get(i).getX();
			d[i][1] = (float) v3d.get(i).getY();
			d[i][2] = (float) v3d.get(i).getZ();
			nonZero |= d[i][0] != 0 || d[i][1] != 0 || d[i][2] != 0;
		}
		if (!nonZero && instances.size() > 1)
			throw new IllegalStateException("No attributes!"); // this is what weka fires as well

		//		System.out.println("before: " + ArrayUtil.toString(d));
		ArrayUtil.normalize(d, -1, 1);
		//		System.out.println("after: " + ArrayUtil.toString(d));

		positions = new ArrayList<Vector3f>();
		for (int i = 0; i < instances.size(); i++)
			positions.add(new Vector3f((float) d[i][0], (float) d[i][1], (float) d[i][2]));
		//v3f[i] = new Vector3f((float) v3d.get(i).getX(), (float) v3d.get(i).getY(), (float) v3d.get(i).getZ());
	}

	@Override
	public Binary getBinary()
	{
		return Settings.RSCRIPT_BINARY;
	}

	public abstract int getMinNumFeatures();

	public abstract int getMinNumInstances();

	public static class TSNEFeature3DEmbedder extends AbstractRTo3DEmbedder
	{

		public int getMinNumFeatures()
		{
			return 2;
		}

		@Override
		public int getMinNumInstances()
		{
			return 2;
		}

		@Override
		public String getRScriptName()
		{
			return "tsne_" + maxNumIterations + "_" + getPerplexity() + "_" + initial_dims;
		}

		@Override
		public String getName()
		{
			return Settings.text("embed.r.tsne");
		}

		@Override
		public String getDescription()
		{
			return Settings.text("embed.r.tsne.desc", Settings.R_STRING);
		}

		private int getPerplexity()
		{
			if (numInstances == -1)
				throw new IllegalStateException("num instances not set before");
			return Math.max(2, Math.min(perplexity.getValue(), (int) (numInstances * 2 / 3.0)));
		}

		IntegerProperty maxNumIterations = new IntegerProperty("Maximum number of iterations (max_iter)", 1000);
		IntegerProperty perplexity = new IntegerProperty("Optimal number of neighbors (perplexity)", 30);
		IntegerProperty initial_dims = new IntegerProperty(
				"The number of dimensions to use in reduction method (initial_dims)", 30);

		@Override
		public Property[] getProperties()
		{
			return new Property[] { maxNumIterations, perplexity, initial_dims };
		}

		@Override
		protected String getRScriptCode()
		{
			return "args <- commandArgs(TRUE)\n" //
					+ "\n" + RScriptUtil.installAndLoadPackage("tsne") + "\n"
					+ "df = read.table(args[1])\n"
					+ "res <- tsne(df, k = 3, perplexity=" + getPerplexity()
					+ ", max_iter="
					+ maxNumIterations.getValue() + ", initial_dims=" + initial_dims.getValue()
					+ ")\n"
					+ "print(res$ydata)\n" + "\n" + "##res <- smacofSphere.dual(df, ndim = 3)\n"
					+ "#print(res$conf)\n"
					+ "#print(class(res$conf))\n" + "\n" + "write.table(res$ydata,args[2]) \n" + "";
		}

	}

	public static class PCAFeature3DEmbedder extends AbstractRTo3DEmbedder
	{
		public int getMinNumFeatures()
		{
			return 1;
		}

		@Override
		public int getMinNumInstances()
		{
			return 2;
		}

		@Override
		public String getRScriptName()
		{
			return "pca";
		}

		@Override
		public String getName()
		{
			return Settings.text("embed.r.pca");
		}

		@Override
		public String getDescription()
		{
			return Settings.text("embed.r.pca.desc", Settings.R_STRING);
		}

		@Override
		protected String getRScriptCode()
		{
			return "args <- commandArgs(TRUE)\n" //
					+ "df = read.table(args[1])\n" //
					//					+ "res <- princomp(df)\n" //
					//					+ "print(res$scores[,1:3])\n" //
					// + "write.table(res$scores[,1:3],args[2]) ";
					+ "res <- prcomp(df)\n" //
					+ "rows <-min(nrow(res$x),3)\n" //
					+ "print(res$x[,1:rows])\n" //
					+ "write.table(res$x[,1:rows],args[2]) ";
		}
	}

	public static class SMACOF3DEmbedder extends AbstractRTo3DEmbedder
	{
		public int getMinNumInstances()
		{
			return 4;
		}

		@Override
		protected String getRScriptName()
		{
			return "smacof_" + maxNumIterations;
		}

		@Override
		public String getName()
		{
			return Settings.text("embed.r.smacof");
		}

		@Override
		public String getDescription()
		{
			return Settings.text("embed.r.smacof.desc", Settings.R_STRING);
		}

		IntegerProperty maxNumIterations = new IntegerProperty("Maximum number of iterations (itmax)", 150);

		@Override
		public Property[] getProperties()
		{
			return new Property[] { maxNumIterations };
		}

		@Override
		protected String getRScriptCode()
		{
			return "args <- commandArgs(TRUE)\n" + "\n" + RScriptUtil.installAndLoadPackage("smacof")
					+ "\n"
					+ "df = read.table(args[1])\n"
					+ "d <- dist(df, method = \"euclidean\")\n" //
					+ "res <- smacofSym(d, ndim = 3, metric = FALSE, ties = \"secondary\", verbose = TRUE, itmax = "
					+ maxNumIterations.getValue() + ")\n" + "#res <- smacofSphere.dual(df, ndim = 3)\n"
					+ "print(res$conf)\n" + "print(class(res$conf))\n" + "write.table(res$conf,args[2]) ";
		}

		@Override
		public int getMinNumFeatures()
		{
			return 1;
		}
	}

}
