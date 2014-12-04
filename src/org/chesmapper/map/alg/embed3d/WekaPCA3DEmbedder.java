package org.chesmapper.map.alg.embed3d;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import org.chesmapper.map.alg.DistanceMeasure;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.chesmapper.map.weka.CompoundArffWriter;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.weka.WekaPropertyUtil;

import weka.attributeSelection.PrincipalComponents;
import weka.core.Instance;
import weka.core.Instances;

public class WekaPCA3DEmbedder extends Abstract3DEmbedder
{
	public static final WekaPCA3DEmbedder INSTANCE = new WekaPCA3DEmbedder(null);
	public static final WekaPCA3DEmbedder INSTANCE_NO_PROBS = new WekaPCA3DEmbedder(new Property[0])
	{
		public Property[] getProperties()
		{
			return null;
		}
	};

	PrincipalComponents pca = new PrincipalComponents();
	Instances resultData;
	Property[] properties;

	private WekaPCA3DEmbedder(Property[] properties)
	{
		if (properties != null)
			this.properties = properties;
		else
			this.properties = WekaPropertyUtil.getProperties(pca, new String[0], new WekaPropertyUtil.DefaultChanger()
			{
				@Override
				public String getName()
				{
					return "centerData";
				}

				@Override
				public Object getAlternateDefaultValue()
				{
					return true;
				}
			});
	}

	@Override
	public List<Vector3f> embed(DatasetFile dataset, List<CompoundData> instances, List<CompoundProperty> features)
			throws Exception //boolean[] trainInstances
	{
		if (this == INSTANCE_NO_PROBS)
			pca.setCenterData(true);
		else
			WekaPropertyUtil.setProperties(pca, properties);

		File f = CompoundArffWriter.writeArffFile(dataset, instances, features);
		BufferedReader reader = new BufferedReader(new FileReader(f));
		Instances data = new Instances(reader);

		//		Instances reducedData = new Instances(data);
		//		for (int i = trainInstances.length - 1; i >= 0; i--)
		//			if (!trainInstances[i])
		//				reducedData.remove(i);
		//		System.out.println("num instances of pca: " + reducedData.size());

		TaskProvider.debug("Apply PCA");
		//		pca.buildEvaluator(reducedData);
		pca.buildEvaluator(data);
		//Settings.LOGGER.debug(pca.toString());
		resultData = pca.transformedData(data);

		List<Vector3f> positions = new ArrayList<Vector3f>();
		for (int i = 0; i < resultData.numInstances(); i++)
		{
			Instance in = resultData.instance(i);
			float x = (float) in.value(0);
			float y = 0;
			if (resultData.numAttributes() > 1)
				y = (float) in.value(1);
			float z = 0;
			if (resultData.numAttributes() > 2)
				z = (float) in.value(2);
			positions.add(new Vector3f(x, y, z));
		}
		return positions;
	}

	@Override
	public DistanceMatrix getFeatureDistanceMatrix()
	{
		if (dist == null)
			dist = new DistanceMatrix(DistanceMeasure.EUCLIDEAN_DISTANCE, EmbedUtil.euclMatrix(instances, features));
		return dist;
	}

	@Override
	protected boolean storesDistances()
	{
		return false;
	}

	@Override
	public Property[] getProperties()
	{
		return properties;
	}

	@Override
	public String getName()
	{
		return Settings.text("embed.weka.pca");
	}

	@Override
	public String getShortName()
	{
		return "pca_weka2"; //default is now center
	}

	@Override
	public String getDescription()
	{
		return Settings.text("embed.weka.pca.desc", Settings.WEKA_STRING) + "\n\n"
				+ "<i>WEKA API documentation:</i> http://weka.sourceforge.net/doc/"
				+ pca.getClass().getName().replaceAll("\\.", "/") + ".html\n\n" + "<i>Internal WEKA description:</i>\n"
				+ pca.globalInfo();
	}

	@Override
	public boolean isLinear()
	{
		return true;
	}

	@Override
	public boolean isLocalMapping()
	{
		return false;
	}

	@Override
	public DistanceMeasure getDistanceMeasure()
	{
		return DistanceMeasure.EUCLIDEAN_DISTANCE;
	}
}
