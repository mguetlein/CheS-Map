package data;

import java.util.ArrayList;
import java.util.HashMap;
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

	private HashMap<MoleculeProperty, Integer> numDistinct = new HashMap<MoleculeProperty, Integer>();

	private int numMultiClusteredCompounds = -1;

	private String clusterAlgorithm;
	private boolean isClusterAlgorithmDisjoint;
	private String embedAlgorithm;

	private String embedQuality;

	//	private MoleculeProperty embedQualityProperty;
	//	private HashMap<MoleculeProperty, MoleculePropertyEmbedQuality> embedQualityPerProp = new HashMap<MoleculeProperty, MoleculePropertyEmbedQuality>();

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

	public void addProperty(MoleculeProperty property, int numDistinct)
	{
		properties.add(property);
		this.numDistinct.put(property, numDistinct);
	}

	public List<MoleculeProperty> getProperties()
	{
		return properties;
	}

	public void addFeature(MoleculeProperty feature, int numDistinct)
	{
		features.add(feature);
		this.numDistinct.put(feature, numDistinct);
	}

	public List<MoleculeProperty> getFeatures()
	{
		return features;
	}

	public int numDistinctValues(MoleculeProperty prop)
	{
		return numDistinct.get(prop);
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

	//	public MoleculeProperty getEmbeddingQualityProperty()
	//	{
	//		return embedQualityProperty;
	//	}
	//
	//	public void setEmbeddingQualityProperty(MoleculeProperty embedQualityProperty)
	//	{
	//		this.embedQualityProperty = embedQualityProperty;
	//	}
	//
	//	public void setEmbeddingQuality(MoleculeProperty p, MoleculePropertyEmbedQuality embedQuality)
	//	{
	//		embedQualityPerProp.put(p, embedQuality);
	//	}
	//
	//	public MoleculePropertyEmbedQuality getEmbeddingQuality(MoleculeProperty p)
	//	{
	//		return embedQualityPerProp.get(p);
	//	}
}
