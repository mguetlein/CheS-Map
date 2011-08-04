package alg;

import gui.Progressable;

import java.util.List;

import data.DatasetFile;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;

public interface FeatureComputer
{
	public void computeFeatures(DatasetFile dataset, Progressable progress);

	public List<MoleculeProperty> getFeatures();

	public List<MoleculeProperty> getProperties();

	public List<CompoundData> getCompounds();
}
