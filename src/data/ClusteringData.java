package data;

import java.util.ArrayList;
import java.util.List;

import util.DistanceMatrix;
import util.ListUtil;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;
import dataInterface.SubstructureSmartsType;

public class ClusteringData
{
	private String name;
	private String sdfFilename;

	private List<MoleculeProperty> features = new ArrayList<MoleculeProperty>();
	private List<MoleculeProperty> properties = new ArrayList<MoleculeProperty>();
	private List<SubstructureSmartsType> substructureSmartsTypes = new ArrayList<SubstructureSmartsType>();

	private boolean clusterFilesAligned;
	private List<ClusterData> clusters = new ArrayList<ClusterData>();
	private DistanceMatrix<ClusterData> clusterDistances;
	private List<CompoundData> compounds = new ArrayList<CompoundData>();
	private DistanceMatrix<CompoundData> compoundDistances;

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

	public ClusteringData(String name, String sdfFilename)
	{
		this.name = name;
		this.sdfFilename = sdfFilename;
	}

	public String getName()
	{
		return name;
	}

	public boolean isClusterFilesAligned()
	{
		return clusterFilesAligned;
	}

	public void setClusterFilesAligned(boolean clusterFilesAligned)
	{
		this.clusterFilesAligned = clusterFilesAligned;
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

	public DistanceMatrix<ClusterData> getClusterDistances()
	{
		if (clusterDistances == null)
			clusterDistances = DistanceUtil.computeDistances(ListUtil.cast(MolecularPropertyOwner.class, clusters),
					features).cast(ClusterData.class);
		return clusterDistances;
	}

	public DistanceMatrix<CompoundData> getCompoundDistances()
	{
		if (compoundDistances == null)
			compoundDistances = DistanceUtil.computeDistances(ListUtil.cast(MolecularPropertyOwner.class, compounds),
					features).cast(CompoundData.class);
		return compoundDistances;
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

}
