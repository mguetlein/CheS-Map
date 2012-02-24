package alg.embed3d;

import gui.binloc.Binary;
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
import data.DatasetFile;
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
			if (!Settings.CACHING_ENABLED || !new File(featureTableFile).exists())
				ExportRUtil.toRTable(features,
						MoleculePropertyUtil.valuesReplaceNullWithMedian(features, instances, dataset),
						featureTableFile);
			else
				System.out.println("load cached features from " + featureTableFile);

			System.out.println("Using r-embedder " + getName() + " with properties: "
					+ PropertyUtil.toString(getProperties()));

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

}
