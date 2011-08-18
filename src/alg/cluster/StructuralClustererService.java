package alg.cluster;

import gui.Progressable;
import gui.property.DoubleProperty;
import gui.property.Property;
import gui.property.StringProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.Settings;
import opentox.DatasetUtil;
import opentox.RESTUtil;
import data.DefaultFeatureComputer;
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
	private String clusterAlgorithm = "http://opentox-dev.informatik.tu-muenchen.de:8080/OpenTox-dev/algorithm/StructuralClustering";
	private double threshold = 0.4;
	List<ClusterData> clusters;

	@Override
	public void clusterDataset(DatasetFile dataset, List<CompoundData> compounds, List<MoleculeProperty> features,
			Progressable progress)
	{
		List<String> urisToDelete = new ArrayList<String>();
		try
		{
			if (dataset.isLocal() || !DatasetUtil.isAmbitURI(dataset.getURI()))
			{
				progress.update(0, "Upload dataset to Ambit dataset web service");
				origURI = DatasetUtil.uploadDatasetToAmbit(dataset.getSDFPath(true));
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

			String modelURI = RESTUtil.post(clusterAlgorithm, params);
			//			urisToDelete.add(modelURI);

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

			if (clusterProps.length == 0)
				throw new Error("Structural clustering produced " + clusterProps.length + " clusters");

			System.out.println("Structural clustering produced " + clusterProps.length + " clusters");
			clusters = new ArrayList<ClusterData>();
			for (int i = 0; i < clusterProps.length; i++)
			{
				ClusterDataImpl c = new ClusterDataImpl();
				clusters.add(c);
			}
			DefaultFeatureComputer featureComp = new DefaultFeatureComputer(clusterProps);
			featureComp.computeFeatures(resultDataset, null);
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
				boolean assigned = false;
				for (int j = 0; j < clusterProps.length; j++)
				{
					if (newCompounds.get(i).getStringValue(clusterProps[j]).equals("1"))
					{
						((ClusterDataImpl) clusters.get(j)).addCompound(compounds.get(i));
						assigned = true;
						break;
					}
				}
				if (!assigned)
					System.err.println("not assigned: " + dataset.getStringValues(CDKProperty.SMILES)[i]);
			}
			DatasetClustererUtil.storeClusters(dataset.getSDFPath(true), "structural", clusters);
		}
		finally
		{
			for (String uri : urisToDelete)
				if (uri != null)
					RESTUtil.delete(uri);
		}
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
	public static final String PROPERTY_SERVICE = "structural clustering webservice";

	@Override
	public Property[] getProperties()
	{
		return new Property[] { new StringProperty(PROPERTY_SERVICE, clusterAlgorithm),
				new DoubleProperty(PROPERTY_THETA, threshold) };
	}

	@Override
	public void setProperties(Property[] properties)
	{
		for (Property property : properties)
		{
			if (property.getName().equals(PROPERTY_THETA))
				threshold = ((DoubleProperty) property).getValue();
			else if (property.getName().equals(PROPERTY_SERVICE))
				clusterAlgorithm = ((StringProperty) property).getValue();
		}
	}

	@Override
	public String getName()
	{
		return "Structural Clusterer Webservice";
	}

	@Override
	public String getDescription()
	{
		return "Structural Clusterer Webservice, located at the TU-München\nWARNING: The data is clustered with an external web service, please use small datsets (< 50 compounds) only.";
	}

	@Override
	public String getPreconditionErrors()
	{
		return null;
	}
}
