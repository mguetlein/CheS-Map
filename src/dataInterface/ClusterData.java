package dataInterface;

import java.util.List;

public interface ClusterData extends MolecularPropertyOwner
{
	public String getName();

	public String getFilename();

	public int getSize();

	public List<CompoundData> getCompounds();

	public String getSubstructureSmarts(SubstructureSmartsType type);

	public boolean isAligned();

	public String getSummaryStringValue(MoleculeProperty p);

	public String getAlignAlgorithm();
}
