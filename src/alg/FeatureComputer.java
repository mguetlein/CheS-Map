package alg;

import java.util.List;

import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;

public interface FeatureComputer
{
	public void computeFeatures(String sdfFilename);

	public List<MoleculeProperty> getFeatures();

	public List<MoleculeProperty> getProperties();

	public List<CompoundData> getCompounds();
}
