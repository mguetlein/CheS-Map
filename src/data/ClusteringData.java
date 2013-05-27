package data;

import java.util.ArrayList;
import java.util.List;

import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;
import dataInterface.SubstructureSmartsType;

public class ClusteringData
{
	private String name;
	private String fullName;
	private String sdfFilename;

	private List<CompoundProperty> features = new ArrayList<CompoundProperty>();
	private List<CompoundProperty> properties = new ArrayList<CompoundProperty>();
	private List<SubstructureSmartsType> substructureSmartsTypes = new ArrayList<SubstructureSmartsType>();

	private List<ClusterData> clusters = new ArrayList<ClusterData>();
	private List<CompoundData> compounds = new ArrayList<CompoundData>();

	private int numMultiClusteredCompounds = -1;

	private String clusterAlgorithm;
	private boolean isClusterAlgorithmDisjoint;
	private String embedAlgorithm;

	private String embedQuality;

	//	private CompoundProperty embedQualityProperty;
	//	private HashMap<CompoundProperty, CompoundPropertyEmbedQuality> embedQualityPerProp = new HashMap<CompoundProperty, CompoundPropertyEmbedQuality>();

	// --------------------------------------------

	public int getNumClusters()
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

	public int getNumCompounds(boolean includingMultiClusteredCompounds)
	{
		if (includingMultiClusteredCompounds)
		{
			if (numMultiClusteredCompounds == -1)
			{
				numMultiClusteredCompounds = 0;
				for (ClusterData c : clusters)
					numMultiClusteredCompounds += c.getSize();
			}
			return numMultiClusteredCompounds;
		}
		else
			return compounds.size();
	}

	public void addProperty(CompoundProperty property)
	{
		properties.add(property);
	}

	public List<CompoundProperty> getProperties()
	{
		return properties;
	}

	public void addFeature(CompoundProperty feature)
	{
		features.add(feature);
	}

	public List<CompoundProperty> getFeatures()
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

	public void setClusterAlgorithmDistjoint(boolean disjoint)
	{
		isClusterAlgorithmDisjoint = disjoint;
	}

	public boolean isClusterAlgorithmDisjoint()
	{
		return isClusterAlgorithmDisjoint;
	}

	public void setEmbedAlgorithm(String embedAlgorithm)
	{
		this.embedAlgorithm = embedAlgorithm;
	}

	public String getEmbedAlgorithm()
	{
		return embedAlgorithm;
	}

	public void setEmbedQuality(String embedQuality)
	{
		this.embedQuality = embedQuality;
	}

	public String getEmbedQuality()
	{
		return embedQuality;
	}

	//	public CompoundProperty getEmbeddingQualityProperty()
	//	{
	//		return embedQualityProperty;
	//	}
	//
	//	public void setEmbeddingQualityProperty(CompoundProperty embedQualityProperty)
	//	{
	//		this.embedQualityProperty = embedQualityProperty;
	//	}
	//
	//	public void setEmbeddingQuality(CompoundProperty p, CompoundPropertyEmbedQuality embedQuality)
	//	{
	//		embedQualityPerProp.put(p, embedQuality);
	//	}
	//
	//	public CompoundPropertyEmbedQuality getEmbeddingQuality(CompoundProperty p)
	//	{
	//		return embedQualityPerProp.get(p);
	//	}
}
