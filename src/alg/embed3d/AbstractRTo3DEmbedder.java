package alg.embed3d;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Message;
import gui.Messages;
import gui.binloc.Binary;
import gui.property.IntegerProperty;
import gui.property.Property;
import gui.property.PropertyUtil;
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
import util.FileUtil;
import alg.AlgorithmException.EmbedException;
import alg.cluster.DatasetClusterer;
import data.DatasetFile;
import data.DistanceUtil;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculePropertyUtil;

public abstract class AbstractRTo3DEmbedder extends Abstract3DEmbedder
{
	protected int numInstances = -1;
	protected int numFeatures = -1;

	protected abstract String getRScriptCode();

	protected abstract String getShortName();

	@Override
	public List<Vector3f> embed(DatasetFile dataset, final List<MolecularPropertyOwner> instances,
			final List<MoleculeProperty> features) throws IOException
	{
		this.numInstances = instances.size();
		this.numFeatures = features.size();

		if (features.size() < getMinNumFeatures())
			throw new EmbedException(this, getShortName() + " requires for embedding at least " + getMinNumFeatures()
					+ " features with non-unique values (num features is '" + features.size() + "')");
		if (instances.size() < getMinNumInstances())
			throw new EmbedException(this, getShortName() + " requires for embedding at least " + getMinNumInstances()
					+ " compounds (num compounds is '" + instances.size() + "')");

		File tmp = File.createTempFile(dataset.getShortName(), "emb");
		try
		{
			String propsMD5 = PropertyUtil.getPropertyMD5(getProperties());
			String datasetMD5 = MoleculePropertyUtil.getSetMD5(features, dataset.getMD5());

			String featureTableFile = Settings.destinationFile(dataset, dataset.getShortName() + "." + datasetMD5
					+ ".features.table");
			if (!new File(featureTableFile).exists())
				ExportRUtil.toRTable(features, DistanceUtil.values(features, instances), featureTableFile);
			else
				System.out.println("load cached features from " + featureTableFile);
			String errorOut = ExternalToolUtil.run(
					getShortName(),
					new String[] {
							Settings.RSCRIPT_BINARY.getLocation(),
							FileUtil.getAbsolutePathEscaped(new File(RScriptUtil.getScriptPath(getShortName() + "."
									+ propsMD5, getRScriptCode()))),
							FileUtil.getAbsolutePathEscaped(new File(featureTableFile)),
							FileUtil.getAbsolutePathEscaped(tmp) });
			if (!tmp.exists())
				throw new IllegalStateException("embedding failed:\n" + errorOut);

			List<Vector3D> v3d = RUtil.readRVectorMatrix(tmp.getAbsolutePath());
			if (v3d.size() != instances.size())
				throw new IllegalStateException("error using '" + getShortName() + "' num results is '" + v3d.size()
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
				throw new IllegalStateException("No attributes!");

			// System.out.println("before: " + ArrayUtil.toString(d));
			ArrayUtil.normalize(d, -1, 1);
			// System.out.println("after: " + ArrayUtil.toString(d));

			List<Vector3f> positions = new ArrayList<Vector3f>();
			for (int i = 0; i < instances.size(); i++)
				positions.add(new Vector3f((float) d[i][0], (float) d[i][1], (float) d[i][2]));
			return positions;
			// v3f[i] = new Vector3f((float) v3d.get(i).getX(), (float) v3d.get(i).getY(), (float) v3d.get(i).getZ());
		}
		finally
		{
			tmp.delete();
		}
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
		public String getShortName()
		{
			return "tsne";
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
			return Math.max(2, Math.min(perplexity.getValue(), (int) numInstances));
		}

		private int getInitialDims()
		{
			if (numFeatures == -1)
				throw new IllegalStateException("num features not set before");
			return Math.max(2, Math.min(initial_dims.getValue(), (int) numFeatures));
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
		public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
		{
			Messages m = super.getMessages(dataset, featureInfo, clusterer);
			if (dataset.numCompounds() >= 50 && featureInfo.isNumFeaturesHigh())
				m.add(Message.slowMessage(featureInfo.getNumFeaturesWarning()));
			return m;
		}

		@Override
		protected String getRScriptCode()
		{
			return "args <- commandArgs(TRUE)\n" //
					+ "\n" + RScriptUtil.installAndLoadPackage("tsne") + "\n"
					+ "df = read.table(args[1])\n"
					+ "res <- tsne(df, k = 3, perplexity=" + getPerplexity()
					+ ", max_iter="
					+ maxNumIterations.getValue() + ", initial_dims=" + getInitialDims() + ")\n"
					+ "print(res$ydata)\n"
					+ "\n" + "##res <- smacofSphere.dual(df, ndim = 3)\n"
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
		public String getShortName()
		{
			return "pca";
		}

		@Override
		public String getName()
		{
			return getNameStatic();
		}

		public static String getNameStatic()
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
					// + "res <- princomp(df)\n" //
					// + "print(res$scores[,1:3])\n" //
					// + "write.table(res$scores[,1:3],args[2]) ";
					+ "res <- prcomp(df)\n" //
					+ "rows <-min(ncol(res$x),3)\n" //
					+ "print(res$x[,1:rows])\n" //
					+ "write.table(res$x[,1:rows],args[2]) ";
		}

	}

	public static class SMACOF3DEmbedder extends AbstractRTo3DEmbedder
	{
		public int getMinNumInstances()
		{
			return 4; // else "Maximum number of dimensions is n-1!"
		}

		@Override
		protected String getShortName()
		{
			return "smacof";
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
		public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
		{
			Messages m = super.getMessages(dataset, featureInfo, clusterer);
			if (dataset.numCompounds() >= 50)
				m.add(Message.slowMessage(Settings.text("embed.r.smacof.slow", PCAFeature3DEmbedder.getNameStatic())));
			return m;
		}

		@Override
		public int getMinNumFeatures()
		{
			return 1;
		}
	}

}
