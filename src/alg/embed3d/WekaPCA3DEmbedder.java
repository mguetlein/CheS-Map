package alg.embed3d;

import gui.binloc.Binary;
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
import data.DatasetFile;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

public class WekaPCA3DEmbedder implements ThreeDEmbedder
{
	PrincipalComponents pca = new PrincipalComponents();
	Instances resultData;
	List<Vector3f> positions;
	Property[] properties = WekaPropertyUtil.getProperties(pca);

	@Override
	public void embed(DatasetFile dataset, List<MolecularPropertyOwner> instances, List<MoleculeProperty> features)
			throws Exception
	{
		WekaPropertyUtil.setProperties(pca, properties);

		if (instances.size() < 2)
			throw new Exception("too few instances");
		File f = CompoundArffWriter.writeArffFile(instances, features);
		BufferedReader reader = new BufferedReader(new FileReader(f));
		Instances data = new Instances(reader);
		pca.buildEvaluator(data);
		resultData = pca.transformedData(data);

		positions = new ArrayList<Vector3f>();
		for (int i = 0; i < resultData.numInstances(); i++)
		{
			Instance in = resultData.get(i);
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
	public Property[] getProperties()
	{
		return properties;
	}

	@Override
	public String getName()
	{
		return "PCA 3D Embedder (WEKA)";
	}

	@Override
	public String getDescription()
	{
		String s = "Uses " + Settings.WEKA_STRING + ".\n";
		s += "Principal component analysis (PCA) is a method that reduces the feature space. The "
				+ "method transforms the orginial features into principal componentes, which are uncorrelated numerical "
				+ "features. The first, most significant 3 principal components are used for 3D Embedding.\n\n";
		s += "<i>WEKA API documentation:</i> http://weka.sourceforge.net/doc/"
				+ pca.getClass().getName().replaceAll("\\.", "/") + ".html\n\n";
		s += "<i>Internal WEKA description:</i>\n" + pca.globalInfo();
		return s;
	}

	@Override
	public Binary getBinary()
	{
		return null;
	}

	@Override
	public boolean requiresFeatures()
	{
		return true;
	}

	@Override
	public List<Vector3f> getPositions()
	{
		return positions;
	}

	@Override
	public String getWarning()
	{
		return null;
	}

}
