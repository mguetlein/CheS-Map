package alg.embed3d;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Messages;
import gui.property.Property;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import main.Settings;
import util.MessageUtil;
import weka.CompoundArffWriter;
import weka.WekaPropertyUtil;
import weka.attributeSelection.PrincipalComponents;
import weka.core.Instance;
import weka.core.Instances;
import alg.DistanceMeasure;
import alg.cluster.DatasetClusterer;
import data.DatasetFile;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;

public class WekaPCA3DEmbedder extends Abstract3DEmbedder
{
	public static final WekaPCA3DEmbedder INSTANCE = new WekaPCA3DEmbedder(null);
	public static final WekaPCA3DEmbedder INSTANCE_NO_PROBS = new WekaPCA3DEmbedder(new Property[0]);

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
			throws Exception
	{
		if (this == INSTANCE_NO_PROBS)
			pca.setCenterData(true);
		else
			WekaPropertyUtil.setProperties(pca, properties);

		File f = CompoundArffWriter.writeArffFile(dataset, instances, features);
		BufferedReader reader = new BufferedReader(new FileReader(f));
		Instances data = new Instances(reader);

		pca.buildEvaluator(data);
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
	protected double[][] getFeatureDistanceMatrix()
	{
		return null;
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		Messages m = super.getMessages(dataset, featureInfo, clusterer);
		if (dataset.numCompounds() >= 50 && featureInfo.isNumFeaturesHigh())
			m.add(MessageUtil.slowMessage(featureInfo.getNumFeaturesWarning()));
		return m;
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
