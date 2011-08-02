package alg.cluster;

import gui.Progressable;
import gui.property.DoubleProperty;
import gui.property.Property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.Settings;
import opentox.DatasetUtil;
import opentox.RESTUtil;
import data.CDKFeatureComputer;
import data.CDKProperty;
import data.ClusterDataImpl;
import data.DatasetFile;
import data.IntegratedProperty;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;

public class StructuralClustererService implements DatasetClusterer
{
	String origURI;
	private static final String ALGORITHM_URI = "http://opentox-dev.informatik.tu-muenchen.de:8080/OpenTox/algorithm/StructuralClustering";
	private double threshold = 0.8;
	List<ClusterData> clusters;

	@Override
	public void clusterDataset(DatasetFile dataset, List<CompoundData> compounds, List<MoleculeProperty> features,
			Progressable progress)
	{
		List<String> urisToDelete = new ArrayList<String>();

		if (dataset.isLocal() || !DatasetUtil.isAmbitURI(dataset.getURI()))
		{
			progress.update(0, "Upload dataset to Ambit dataset web service");
			origURI = DatasetUtil.uploadDatasetToAmbit(dataset.getSDFPath());
			urisToDelete.add(origURI);
		}
		else
			origURI = dataset.getURI();

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("threshold", threshold + "");
		params.put("dataset_uri", origURI);

		if (Settings.isAborted(Thread.currentThread()))
			return;
		progress.update(10, "Building cluster model with TUM algorithm web service");

		String modelURI = RESTUtil.post(ALGORITHM_URI, params);
		urisToDelete.add(modelURI);

		if (Settings.isAborted(Thread.currentThread()))
			return;
		progress.update(50, "Clustering dataset with TUM model web service");

		String resultDatasetURI = RESTUtil.post(modelURI, params);
		urisToDelete.add(resultDatasetURI);

		if (Settings.isAborted(Thread.currentThread()))
			return;
		progress.update(90, "Downloading clustered dataset");

		DatasetFile resultDataset = DatasetFile.getURLDataset(resultDatasetURI);
		DatasetUtil.downloadDataset(resultDataset.getURI());
		try
		{
			resultDataset.loadDataset();
		}
		catch (Exception e)
		{
			throw new Error(e);
		}

		int size = dataset.numCompounds();

		if (compounds.size() != size)
			throw new IllegalArgumentException();

		IntegratedProperty clusterProps[] = resultDataset.getIntegratedClusterProperties();
		System.out.println("structural clustering produced " + clusterProps.length + " clusters");
		clusters = new ArrayList<ClusterData>();
		for (int i = 0; i < clusterProps.length; i++)
		{
			ClusterDataImpl c = new ClusterDataImpl();
			clusters.add(c);
		}
		CDKFeatureComputer featureComp = new CDKFeatureComputer(clusterProps, null);
		featureComp.computeFeatures(resultDataset);
		List<CompoundData> newCompounds = featureComp.getCompounds();

		for (int i = 0; i < size; i++)
		{
			String smiles1 = resultDataset.getStringValues(CDKProperty.SMILES)[i];
			String smiles2 = dataset.getStringValues(CDKProperty.SMILES)[i];

			System.err.println(i);
			System.err.println(smiles1 + " " + smiles2);
			if (!smiles1.equals(smiles2))

				throw new Error("result dataset has different compounds according to cdk-smiles");
		}
		System.err.println("WHAT TO DO ABOUT MULTIPLE ASSIGNMENTS?");
		for (int i = 0; i < compounds.size(); i++)
		{
			for (int j = 0; j < clusterProps.length; j++)
			{
				if (newCompounds.get(i).getValue(clusterProps[j], false) == 1)
				{
					((ClusterDataImpl) clusters.get(j)).addCompound(compounds.get(i));
					break;
				}
			}
		}
		DatasetClustererUtil.storeClusters(dataset.getSDFPath(), "structural", clusters);

		for (String uri : urisToDelete)
			RESTUtil.delete(uri);
	}

	@Override
	public List<ClusterData> getClusters()
	{
		return clusters;
	}

	@Override
	public boolean requiresNumericalFeatures()
	{
		return false;
	}

	public static final String PROPERTY_THETA = "theta (size of common substructure)";

	@Override
	public Property[] getProperties()
	{
		return new Property[] { new DoubleProperty(PROPERTY_THETA, threshold) };
	}

	@Override
	public void setProperties(Property[] properties)
	{
		for (Property property : properties)
			if (property.getName().equals(PROPERTY_THETA))
				threshold = ((DoubleProperty) property).getValue();
	}

	@Override
	public String getName()
	{
		return "Structural Clusterer Webservice";
	}

	@Override
	public String getDescription()
	{
		return "TUM werbservice";
	}

	@Override
	public String getPreconditionErrors()
	{
		return null;
	}
}
