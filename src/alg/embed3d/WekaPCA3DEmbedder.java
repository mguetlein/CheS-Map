package alg.embed3d;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Message;
import gui.Messages;
import gui.property.Property;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import main.Settings;
import weka.CompoundArffWriter;
import weka.WekaPropertyUtil;
import weka.attributeSelection.PrincipalComponents;
import weka.core.Instance;
import weka.core.Instances;
import alg.cluster.DatasetClusterer;
import data.DatasetFile;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

public class WekaPCA3DEmbedder extends Abstract3DEmbedder
{
	PrincipalComponents pca = new PrincipalComponents();
	Instances resultData;
	Property[] properties;

	public WekaPCA3DEmbedder()
	{
		this(false);
	}

	public WekaPCA3DEmbedder(boolean withProps)
	{
		if (withProps)
			properties = WekaPropertyUtil.getProperties(pca);
	}

	@Override
	public void embed(DatasetFile dataset, List<MolecularPropertyOwner> instances, List<MoleculeProperty> features)
			throws Exception
	{
		if (properties != null)
			WekaPropertyUtil.setProperties(pca, properties);

		File f = CompoundArffWriter.writeArffFile(dataset, instances, features);
		BufferedReader reader = new BufferedReader(new FileReader(f));
		Instances data = new Instances(reader);

		pca.buildEvaluator(data);
		resultData = pca.transformedData(data);

		positions = new ArrayList<Vector3f>();
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
	public String getDescription()
	{
		return Settings.text("embed.weka.pca.desc", Settings.WEKA_STRING) + "\n\n"
				+ "<i>WEKA API documentation:</i> http://weka.sourceforge.net/doc/"
				+ pca.getClass().getName().replaceAll("\\.", "/") + ".html\n\n" + "<i>Internal WEKA description:</i>\n"
				+ pca.globalInfo();
	}

}
