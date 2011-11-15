package data;

import java.util.ArrayList;
import java.util.List;

import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;
import dataInterface.SubstructureSmartsType;

public class ClusteringData
{
	private String name;
	private String fullName;
	private String sdfFilename;

	private List<MoleculeProperty> features = new ArrayList<MoleculeProperty>();
	private List<MoleculeProperty> properties = new ArrayList<MoleculeProperty>();
	private List<SubstructureSmartsType> substructureSmartsTypes = new ArrayList<SubstructureSmartsType>();

	private List<ClusterData> clusters = new ArrayList<ClusterData>();
	private List<CompoundData> compounds = new ArrayList<CompoundData>();

	private String clusterAlgorithm;
	private String embedAlgorithm;

	// --------------------------------------------

	public int getSize()
	{
		return clusters.size();
	}

	public ClusterData getCluster(int index)
	{
		return clusters.get(index);
	}

	// --------------------------------------------

	public ClusteringData(String name, String fullName, String sdfFilename)
	{
		this.name = name;
		this.fullName = fullName;
		this.sdfFilename = sdfFilename;
	}

	public String getName()
	{
		return name;
	}

	public String getFullName()
	{
		return fullName;
	}

	public void addCluster(ClusterData cluster)
	{
		clusters.add(cluster);
	}

	public List<ClusterData> getClusters()
	{
		return clusters;
	}

	public void addCompound(CompoundData compound)
	{
		compounds.add(compound);
	}

	public List<CompoundData> getCompounds()
	{
		return compounds;
	}

	public void addProperty(MoleculeProperty property)
	{
		properties.add(property);
	}

	public List<MoleculeProperty> getProperties()
	{
		return properties;
	}

	public void addFeature(MoleculeProperty feature)
	{
		features.add(feature);
	}

	public List<MoleculeProperty> getFeatures()
	{
		return features;
	}

	public String getSDFFilename()
	{
		return sdfFilename;
	}

	public List<SubstructureSmartsType> getSubstructureSmartsTypes()
	{
		return substructureSmartsTypes;
	}

	public void addSubstructureSmartsTypes(SubstructureSmartsType type)
	{
		substructureSmartsTypes.add(type);
	}

	public void setClusterAlgorithm(String clusterAlgorithm)
	{
		this.clusterAlgorithm = clusterAlgorithm;
	}

	public String getClusterAlgorithm()
	{
		return clusterAlgorithm;
	}

	public void setEmbedAlgorithm(String embedAlgorithm)
	{
		this.embedAlgorithm = embedAlgorithm;
	}

	public String getEmbedAlgorithm()
	{
		return embedAlgorithm;
	}

}
