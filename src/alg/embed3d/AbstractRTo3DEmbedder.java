package alg.embed3d;

import gui.Message;
import gui.binloc.Binary;
import gui.property.PropertyUtil;
import io.RUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import main.BinHandler;
import main.Settings;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import rscript.ExportRUtil;
import util.ArrayUtil;
import util.ExternalToolUtil;
import util.FileUtil;
import alg.AlgorithmException.EmbedException;
import data.DatasetFile;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundPropertyUtil;

public abstract class AbstractRTo3DEmbedder extends Abstract3DEmbedder
{
	//	private double[][] featureDistanceMatrix;

	protected abstract String getRScriptCode();

	protected abstract String getShortName();

	protected static String TOO_FEW_UNIQUE_DATA_POINTS = "Too few unique data points, add features or choose a different embedder.";

	protected abstract String getErrorDescription(String errorOut);

	@Override
	public List<Vector3f> embed(DatasetFile dataset, final List<CompoundData> instances,
			final List<CompoundProperty> features) throws IOException
	{
		processMessages.clear();
		if (features.size() < getMinNumFeatures())
			throw new EmbedException(this, getShortName() + " requires for embedding at least " + getMinNumFeatures()
					+ " features with non-unique values (num features is '" + features.size() + "')");
		if (instances.size() < getMinNumInstances())
			throw new EmbedException(this, getShortName() + " requires for embedding at least " + getMinNumInstances()
					+ " compounds (num compounds is '" + instances.size() + "')");

		File tmp = File.createTempFile(dataset.getShortName(), "emb");
		File tmpDist = File.createTempFile(dataset.getShortName(), "embDist");
		File tmpInfo = File.createTempFile(dataset.getShortName(), "embInfo");
		File rScript = null;
		try
		{
			String featureTableFile = dataset.getFeatureTableFilePath("table");
			if (!Settings.CACHING_ENABLED || !new File(featureTableFile).exists())
				ExportRUtil.toRTable(features,
						CompoundPropertyUtil.valuesReplaceNullWithMedian(features, instances, dataset),
						featureTableFile);
			else
				Settings.LOGGER.info("load cached features from " + featureTableFile);

			Settings.LOGGER.info("Using r-embedder " + getName() + " with properties: "
					+ PropertyUtil.toString(getProperties()));

			rScript = File.createTempFile("rscript", "R");
			FileUtil.writeStringToFile(rScript.getAbsolutePath(), getRScriptCode());

			String errorOut = ExternalToolUtil.run(
					getShortName(),
					new String[] { BinHandler.RSCRIPT_BINARY.getLocation(), FileUtil.getAbsolutePathEscaped(rScript),
							FileUtil.getAbsolutePathEscaped(new File(featureTableFile)),
							FileUtil.getAbsolutePathEscaped(tmp), FileUtil.getAbsolutePathEscaped(tmpDist),
							FileUtil.getAbsolutePathEscaped(tmpInfo) });
			if (!tmp.exists())
				throw new IllegalStateException("embedding failed:\n" + errorOut);

			try
			{
				processMessages.add(Message.infoMessage(FileUtil.readStringFromFile(tmpInfo.getAbsolutePath())));
			}
			catch (Exception e)
			{
				Settings.LOGGER.warn("could not read info from embbeding algorithm");
			}

			List<Vector3D> v3d = RUtil.readRVectorMatrix(tmp.getAbsolutePath());
			if (v3d.size() != instances.size())
			{
				String desc = getErrorDescription(errorOut);
				if (desc != null)
					throw new EmbedException(this, desc);
				else
					throw new IllegalStateException(errorOut);
			}
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

			// Settings.LOGGER.println("before: " + ArrayUtil.toString(d));
			ArrayUtil.normalize(d, -1, 1);
			// Settings.LOGGER.println("after: " + ArrayUtil.toString(d));

			List<Vector3f> positions = new ArrayList<Vector3f>();
			for (int i = 0; i < instances.size(); i++)
				positions.add(new Vector3f((float) d[i][0], (float) d[i][1], (float) d[i][2]));

			FileUtil.robustRenameTo(tmpDist.getAbsolutePath(), dataset.getEmbeddingResultsFilePath("dist"));
			return positions;
		}
		finally
		{
			tmp.delete();
			tmpDist.delete();
			tmpInfo.delete();
			if (rScript != null)
				rScript.delete();
		}
	}

	@Override
	protected boolean storesDistances()
	{
		return true;
	}

	@Override
	public DistanceMatrix getFeatureDistanceMatrix()
	{
		if (dist == null)
			dist = new DistanceMatrix(getDistanceMeasure(), RUtil.readMatrix(
					dataset.getEmbeddingResultsFilePath("dist"), 0));
		return dist;
	}

	@Override
	public Binary getBinary()
	{
		return BinHandler.RSCRIPT_BINARY;
	}

	public abstract int getMinNumFeatures();

	public abstract int getMinNumInstances();

}
