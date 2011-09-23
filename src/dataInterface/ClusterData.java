package dataInterface;

import java.util.List;

import javax.vecmath.Vector3f;

public interface ClusterData extends MolecularPropertyOwner
{
	public String getName();

	public String getFilename();

	public int getSize();

	public List<CompoundData> getCompounds();

	public Vector3f getPosition();

	public String getSubstructureSmarts(SubstructureSmartsType type);

	public boolean isAligned();

	public String getSummaryStringValue(MoleculeProperty p);

	public String getClusterAlgorithm();

	public String getEmbedAlgorithm();

	public String getAlignAlgorithm();
}
