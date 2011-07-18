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
import data.DistanceUtil;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

public abstract class AbstractRFeatureTo3DEmbedder implements ThreeDEmbedder
{
	List<Vector3f> positions;

	Random3DEmbedder random = new Random3DEmbedder();

	@Override
	public boolean requiresNumericalFeatures()
	{
		return true;
	}

	@Override
	public void embed(String filename, List<MolecularPropertyOwner> instances, List<MoleculeProperty> features,
			final DistanceMatrix<MolecularPropertyOwner> distances)
	{
		if (features.size() < getMinNumFeatures())
		{
			System.out.println("WARNING: " + getRScript() + " needs at least " + getMinNumFeatures()
					+ " features for embedding, returning 0-0-0 positions");
			random.embed(filename, instances, features, distances);
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

		RUtil.toRTable(features, DistanceUtil.values(features, instances, true), f.getAbsolutePath());

		ExternalTool.run(getRScript(), null, null, Settings.CV_RSCRIPT_PATH + " /home/martin/software/R/"
				+ getRScript() + " " + f.getAbsolutePath() + " " + f2.getAbsolutePath());

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

	public abstract String getRScript();

	public abstract int getMinNumFeatures();

	public static class TSNEFeature3DEmbedder extends AbstractRFeatureTo3DEmbedder
	{

		public int getMinNumFeatures()
		{
			return 2;
		}

		@Override
		public String getRScript()
		{
			return "tsne.R";
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
	}

	public static class PCAFeature3DEmbedder extends AbstractRFeatureTo3DEmbedder
	{
		public int getMinNumFeatures()
		{
			return 3;
		}

		@Override
		public String getRScript()
		{
			return "pca.R";
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
	}

}
