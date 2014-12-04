package org.chesmapper.map.data;

import java.util.ArrayList;
import java.util.List;

import org.chesmapper.map.alg.DistanceMeasure;
import org.chesmapper.map.alg.align3d.ThreeDAligner;
import org.chesmapper.map.alg.build3d.ThreeDBuilder;
import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.alg.embed3d.CorrelationProperty;
import org.chesmapper.map.alg.embed3d.DistanceMatrix;
import org.chesmapper.map.alg.embed3d.EqualPositionProperty;
import org.chesmapper.map.alg.embed3d.ThreeDEmbedder;
import org.chesmapper.map.dataInterface.ClusterData;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.mg.javalib.task.TaskDialog;

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

	private CorrelationProperty embedQualityProperty;
	private EqualPositionProperty equalPosProperty;
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
					numMultiClusteredCompounds += c.getNumCompounds();
			}
			return numMultiClusteredCompounds;
		}
		else
			return compounds.size();
	}

	public void addProperty(CompoundProperty property)
	{
		if (features.indexOf(property) != -1)
			throw new IllegalStateException();
		properties.add(property);
	}

	public List<CompoundProperty> getProperties()
	{
		return properties;
	}

	public void addFeature(CompoundProperty feature)
	{
		if (properties.indexOf(feature) != -1)
			throw new IllegalStateException();
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

	public void addEqualPosProperty(EqualPositionProperty p)
	{
		additionalProperties.add(p);
		equalPosProperty = p;
	}

	public void addEmbedQualityProperty(CorrelationProperty p)
	{
		additionalProperties.add(p);
		embedQualityProperty = p;
	}

	public EqualPositionProperty getEqualPosProperty()
	{
		return equalPosProperty;
	}

	public CorrelationProperty getEmbeddingQualityProperty()
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

	TaskDialog d;

	public void setCheSMappingWarningOwner(TaskDialog d)
	{
		this.d = d;
	}

	public boolean doCheSMappingWarningsExist()
	{
		return d.doWarningsExist();
	}

	public void showCheSMappingWarnings()
	{
		d.showWarningDialog();
	}
}
