package alg.embed3d;

import gui.property.Property;
import io.ExternalTool;
import io.RUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import main.Settings;

import org.apache.commons.math.geometry.Vector3D;

import util.ArrayUtil;
import util.DistanceMatrix;
import data.DatasetFile;
import data.DistanceUtil;
import data.RScriptUser;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

public abstract class AbstractRFeatureTo3DEmbedder extends RScriptUser implements ThreeDEmbedder
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
		if (features.size() < getMinNumFeatures())
		{
			System.out.println("WARNING: " + getRScriptName() + " needs at least " + getMinNumFeatures()
					+ " features for embedding, returning 0-0-0 positions");
			random.embed(dataset, instances, features, distances);
			positions = random.positions;
			return;
		}

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

		ExternalTool.run(
				getRScriptName(),
				null,
				null,
				Settings.CV_RSCRIPT_PATH + " " + getScriptPath() + " " + f.getAbsolutePath() + " "
						+ f2.getAbsolutePath());

		List<Vector3D> v3d = RUtil.readRVectorMatrix(f2.getAbsolutePath());

		double d[][] = new double[v3d.size()][3];
		for (int i = 0; i < v3d.size(); i++)
		{
			d[i][0] = (float) v3d.get(i).getX();
			d[i][1] = (float) v3d.get(i).getY();
			d[i][2] = (float) v3d.get(i).getZ();
		}

		//		System.out.println("before: " + ArrayUtil.toString(d));
		ArrayUtil.normalize(d, -1, 1);
		//		System.out.println("after: " + ArrayUtil.toString(d));

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
		return false;
	}

	public abstract int getMinNumFeatures();

	public static class TSNEFeature3DEmbedder extends AbstractRFeatureTo3DEmbedder
	{

		public int getMinNumFeatures()
		{
			return 2;
		}

		@Override
		public String getRScriptName()
		{
			return "tsne";
		}

		@Override
		public String getName()
		{
			return "TSNE 3D-Embedder (using Features)";
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

	public static class PCAFeature3DEmbedder extends AbstractRFeatureTo3DEmbedder
	{
		public int getMinNumFeatures()
		{
			return 3;
		}

		@Override
		public String getRScriptName()
		{
			return "pca";
		}

		@Override
		public String getDescription()
		{
			return null;
		}

		@Override
		public String getName()
		{
			return "PCA 3D-Embedder (using Features)";
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

}
