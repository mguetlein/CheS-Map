package data;

import java.util.ArrayList;
import java.util.List;

import alg.DistanceMeasure;
import alg.align3d.ThreeDAligner;
import alg.build3d.ThreeDBuilder;
import alg.cluster.DatasetClusterer;
import alg.embed3d.DistanceMatrix;
import alg.embed3d.ThreeDEmbedder;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;

public class ClusteringData
{
	private String name;
	private String fullName;
	private String origLocalPath;
	private String sdf; //input for jmol (3d, aligned)
	private String origSDF; //orig file

	private List<CompoundProperty> features = new ArrayList<CompoundProperty>();
	private List<CompoundProperty> properties = new ArrayList<CompoundProperty>();
	private List<CompoundProperty> additionalProperties = new ArrayList<CompoundProperty>();

	private List<ClusterData> clusters = new ArrayList<ClusterData>();
	private List<CompoundData> compounds = new ArrayList<CompoundData>();

	private int numMultiClusteredCompounds = -1;
	private String embedQuality;

	private CompoundProperty embedQualityProperty;
	//	private CompoundProperty appDomainProperties[];
	//	private List<CompoundProperty> distanceToProperties;

	private ThreeDBuilder threeDBuilder;
	private DatasetClusterer datasetClusterer;
	private ThreeDEmbedder threeDEmbedder;
	private ThreeDAligner threeDAligner;

	private boolean skippingRedundantFeatures;

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

	public ClusteringData(DatasetFile dataset)
	{
		name = dataset.getName();
		fullName = dataset.getFullName();
		origSDF = dataset.getSDF();
		origLocalPath = dataset.isLocal() ? dataset.getLocalPath() : null;
		dataset.setClusteringData(this);
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

	public void setSDF(String sdf)
	{
		this.sdf = sdf;
	}

	public String getSDF()
	{
		return sdf;
	}

	public String getOrigSDF()
	{
		return origSDF;
	}

	public void setEmbedQuality(String embedQuality)
	{
		this.embedQuality = embedQuality;
	}

	public String getEmbedQuality()
	{
		return embedQuality;
	}

	public String getOrigLocalPath()
	{
		return origLocalPath;
	}

	public List<CompoundProperty> getAdditionalProperties()
	{
		return additionalProperties;
	}

	public void addAdditionalProperty(CompoundProperty p, boolean isEmbedQuality)
	{
		additionalProperties.add(p);
		if (isEmbedQuality)
			embedQualityProperty = p;
	}

	public CompoundProperty getEmbeddingQualityProperty()
	{
		return embedQualityProperty;
	}

	public void setThreeDBuilder(ThreeDBuilder threeDBuilder)
	{
		this.threeDBuilder = threeDBuilder;
	}

	public ThreeDBuilder getThreeDBuilder()
	{
		return threeDBuilder;
	}

	public void setDatasetClusterer(DatasetClusterer datasetClusterer)
	{
		this.datasetClusterer = datasetClusterer;
	}

	public DatasetClusterer getDatasetClusterer()
	{
		return datasetClusterer;
	}

	public void setThreeDEmbedder(ThreeDEmbedder threeDEmbedder)
	{
		this.threeDEmbedder = threeDEmbedder;
	}

	public ThreeDEmbedder getThreeDEmbedder()
	{
		return threeDEmbedder;
	}

	public void setThreeDAligner(ThreeDAligner threeDAligner)
	{
		this.threeDAligner = threeDAligner;
	}

	public ThreeDAligner getThreeDAligner()
	{
		return threeDAligner;
	}

	public Double getFeatureDistance(int i1, int i2)
	{
		return threeDEmbedder.getFeatureDistanceMatrix().getValues()[i1][i2];
	}

	public DistanceMatrix getFeatureDistanceMatrix()
	{
		return threeDEmbedder.getFeatureDistanceMatrix();
	}

	public DistanceMeasure getEmbeddingDistanceMeasure()
	{
		return threeDEmbedder.getDistanceMeasure();
	}

	public void setSkippingRedundantFeatures(boolean skippingRedundantFeatures)
	{
		this.skippingRedundantFeatures = skippingRedundantFeatures;
	}

	public boolean isSkippingRedundantFeatures()
	{
		return skippingRedundantFeatures;
	}
}
