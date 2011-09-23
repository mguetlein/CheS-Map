package alg.cluster;

import gui.binloc.Binary;
import gui.property.DoubleProperty;
import gui.property.Property;
import gui.property.StringProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.TaskProvider;
import opentox.DatasetUtil;
import opentox.RESTUtil;
import data.ClusterDataImpl;
import data.DatasetFile;
import data.DefaultFeatureComputer;
import data.IntegratedProperty;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;

public class StructuralClustererService implements DatasetClusterer
{
	String origURI;
	private final String clusterAlgorithmDefault = "http://opentox-dev.informatik.tu-muenchen.de:8080/OpenTox-dev/algorithm/StructuralClustering";
	private String clusterAlgorithm = clusterAlgorithmDefault;
	private final double thresholdDefault = 0.4;
	private double threshold = thresholdDefault;
	List<ClusterData> clusters;

	@Override
	public void clusterDataset(DatasetFile dataset, List<CompoundData> compounds, List<MoleculeProperty> features)
	{
		List<String> urisToDelete = new ArrayList<String>();
		try
		{
			if (dataset.isLocal() || !DatasetUtil.isAmbitURI(dataset.getURI()))
			{
				TaskProvider.task().update("Upload dataset to Ambit dataset web service");
				origURI = DatasetUtil.uploadDatasetToAmbit(dataset.getSDFPath(true));
				urisToDelete.add(origURI);
			}
			else
				origURI = dataset.getURI();

			HashMap<String, String> params = new HashMap<String, String>();
			params.put("threshold", threshold + "");
			params.put("dataset_uri", origURI);

			if (TaskProvider.task().isCancelled())
				return;
			TaskProvider.task().update("Building cluster model with TUM algorithm web service");

			String modelURI = RESTUtil.post(clusterAlgorithm, params);
			//			urisToDelete.add(modelURI);

			if (TaskProvider.task().isCancelled())
				return;
			TaskProvider.task().update("Clustering dataset with TUM model web service");

			String resultDatasetURI = RESTUtil.post(modelURI, params);
			urisToDelete.add(resultDatasetURI);

			if (TaskProvider.task().isCancelled())
				return;
			TaskProvider.task().update("Downloading clustered dataset");

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
			List<CompoundData> newCompounds = featureComp.getCompounds();
			featureComp.computeFeatures(resultDataset);

			for (int i = 0; i < size; i++)
			{
				String smiles1 = dataset.getSmiles()[i];
				String smiles2 = dataset.getSmiles()[i];

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
					System.err.println("not assigned: " + dataset.getSmiles()[i]);
			}
			DatasetClustererUtil.storeClusters(dataset.getSDFPath(true), "structural", getName(), clusters);
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
	public boolean requiresFeatures()
	{
		return false;
	}

	public static final String PROPERTY_THETA = "theta (size of common substructure)";
	public static final String PROPERTY_SERVICE = "structural clustering webservice";

	private Property[] properties = new Property[] {
			new StringProperty(PROPERTY_SERVICE, clusterAlgorithm, clusterAlgorithmDefault),
			new DoubleProperty(PROPERTY_THETA, threshold, thresholdDefault) };

	@Override
	public Property[] getProperties()
	{
		return properties;
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
		return "Structural Clustering Webservice";
	}

	@Override
	public String getDescription()
	{
		return "This is an OpenTox Webservice, provided by the Technical University Munich (TUM).\n\n"
				+ "Integrated substructure mining using gSpan. Compounds are assigned to clusters when there exists a common subgraph of sufficient size.\n\n"
				+ "M. Seeland, T. Girschick, F. Buchwald, and S. Kramer\n"
				+ "Online structural graph clustering using frequent subgraph mining.\n"
				+ "In: Proceedings of the 2010 European Conference on Machine Learning and Knowledge "
				+ "Discovery in Databases, vol. 3, pages 213–228, 2010.";
	}

	@Override
	public Binary getBinary()
	{
		return null;
	}

	@Override
	public String getFixedNumClustersProperty()
	{
		return null;
	}

	@Override
	public String getWarning()
	{
		return "Your data will be send over the internet to an external webserice.\n"
				+ "The service ignores the features selected in the previous step (see description below).\n"
				+ "At present the service can handle only small datsets (< 50 compounds).";
	}
}
