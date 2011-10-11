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

import rscript.RScriptUtil;
import util.ArrayUtil;
import util.DistanceMatrix;
import util.ExternalToolUtil;
import data.DatasetFile;
import data.DistanceUtil;
import data.RScriptUser;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

public abstract class AbstractRFeatureTo3DEmbedder extends RScriptUser implements ThreeDEmbedder
{
	List<Vector3f> positions;

	protected int numInstances = -1;

	@Override
	public boolean requiresFeatures()
	{
		return true;
	}

	@Override
	public String getWarning()
	{
		return null;
	}

	@Override
	public void embed(DatasetFile dataset, List<MolecularPropertyOwner> instances, List<MoleculeProperty> features,
			final DistanceMatrix<MolecularPropertyOwner> distances)
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

		RUtil.toRTable(features, DistanceUtil.values(features, instances), f.getAbsolutePath());

		ExternalToolUtil.run(
				getRScriptName(),
				Settings.RSCRIPT_BINARY.getLocation() + " " + getScriptPath() + " " + f.getAbsolutePath() + " "
						+ f2.getAbsolutePath());

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

	@Override
	public List<Vector3f> getPositions()
	{
		return positions;
	}

	@Override
	public Property[] getProperties()
	{
		return null;
	}

	@Override
	public void setProperties(Property[] properties)
	{

	}

	@Override
	public boolean requiresDistances()
	{
		return false;
	}

	public abstract int getMinNumFeatures();

	public abstract int getMinNumInstances();

	public static class TSNEFeature3DEmbedder extends AbstractRFeatureTo3DEmbedder
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
			return "TSNE 3D Embedder (RScript)";
		}

		@Override
		public String getDescription()
		{
			return "Uses " + Settings.R_STRING + ".\n\n" + "The Embedding is done with the t-SNE algorithm."
					+ "\n(The following R-library is required: http://cran.r-project.org/web/packages/tsne)\n";
		}

		private int getPerplexity()
		{
			if (numInstances == -1)
				throw new IllegalStateException("num instances not set before");
			return 3;
			//return Math.max(2, Math.min(perplexity, (int) (numInstances * 2 / 3.0)));
		}

		private final int maxNumIterationsDefault = 1000;
		private final int initial_dimsDefault = 30;
		private final int perplexityDefault = 30;

		private int maxNumIterations = maxNumIterationsDefault;
		private int initial_dims = initial_dimsDefault;
		private int perplexity = perplexityDefault;

		public static final String PROPERTY_MAX_NUM_ITERATIONS = "Maximum number of iterations (max_iter)";
		public static final String PROPERTY_PERPLEXITY = "Optimal number of neighbors (perplexity)";
		public static final String PROPERTY_INITIAL_DIMS = "The number of dimensions to use in reduction method (initial_dims)";

		private Property[] properties = new Property[] {
				new IntegerProperty(PROPERTY_MAX_NUM_ITERATIONS, maxNumIterations, maxNumIterationsDefault),
				new IntegerProperty(PROPERTY_PERPLEXITY, perplexity, perplexityDefault),
				new IntegerProperty(PROPERTY_INITIAL_DIMS, initial_dims, initial_dimsDefault) };

		@Override
		public Property[] getProperties()
		{
			return properties;
		}

		@Override
		public void setProperties(Property[] properties)
		{
			for (Property property : properties)
			{
				if (property.getName().equals(PROPERTY_MAX_NUM_ITERATIONS))
					maxNumIterations = ((IntegerProperty) property).getValue();
				if (property.getName().equals(PROPERTY_PERPLEXITY))
					perplexity = ((IntegerProperty) property).getValue();
				if (property.getName().equals(PROPERTY_INITIAL_DIMS))
					initial_dims = ((IntegerProperty) property).getValue();
			}
		}

		@Override
		protected String getRScriptCode()
		{
			return "args <- commandArgs(TRUE)\n" //
					+ "\n" + RScriptUtil.installAndLoadPackage("tsne") + "\n"
					+ "df = read.table(args[1])\n"
					+ "res <- tsne(df, k = 3, perplexity=" + getPerplexity() + ", max_iter="
					+ maxNumIterations
					+ ", initial_dims=" + initial_dims + ")\n" + "print(res$ydata)\n"
					+ "\n"
					+ "##res <- smacofSphere.dual(df, ndim = 3)\n" + "#print(res$conf)\n"
					+ "#print(class(res$conf))\n"
					+ "\n" + "write.table(res$ydata,args[2]) \n" + "";
		}

	}

	public static class PCAFeature3DEmbedder extends AbstractRFeatureTo3DEmbedder
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
		public String getDescription()
		{
			return "Uses " + Settings.R_STRING + ".\n\n"
					+ "The first 3 principal components are employed as 3D coordinates.";
		}

		@Override
		public String getName()
		{
			return "PCA 3D Embedder (RScript)";
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

}
