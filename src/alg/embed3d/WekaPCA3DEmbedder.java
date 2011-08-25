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
import util.DistanceMatrix;
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
	Random3DEmbedder random = new Random3DEmbedder();
	Instances resultData;
	List<Vector3f> positions;

	@Override
	public void embed(DatasetFile dataset, List<MolecularPropertyOwner> instances, List<MoleculeProperty> features,
			DistanceMatrix<MolecularPropertyOwner> distances)
	{
		try
		{
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
		catch (Exception e)
		{
			//e.printStackTrace();
			System.err.println("WARNING: WekaPCA3DEmbedder failed: '" + e.getMessage()
					+ "', returning random 3d positions");
			random.embed(dataset, instances, features, distances);
			positions = random.positions;
		}
	}

	@Override
	public Property[] getProperties()
	{
		return WekaPropertyUtil.getProperties(pca);
	}

	@Override
	public void setProperties(Property[] properties)
	{
		WekaPropertyUtil.setProperties(pca, properties);
	}

	@Override
	public String getName()
	{
		return "PCA 3D-Embedder (WEKA)";
	}

	@Override
	public String getDescription()
	{
		return "Uses " + Settings.WEKA_STRING + ".\n\n"
				+ "The first 3 principal components are employed as 3D coordinates.\n\n" + pca.globalInfo();
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
	public boolean requiresDistances()
	{
		return false;
	}

	@Override
	public List<Vector3f> getPositions()
	{
		return positions;
	}

}
