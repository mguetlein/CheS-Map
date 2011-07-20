package alg;

import java.util.List;

import data.DatasetFile;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;

public interface FeatureComputer
{
	public void computeFeatures(DatasetFile dataset);

	public List<MoleculeProperty> getFeatures();

	public List<MoleculeProperty> getProperties();

	public List<CompoundData> getCompounds();
}
