package alg;

import java.util.List;

import data.DatasetFile;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;

public interface FeatureComputer
{
	public void computeFeatures(DatasetFile dataset);

	public List<CompoundProperty> getFeatures();

	public List<CompoundProperty> getProperties();

	public List<CompoundData> getCompounds();
}
