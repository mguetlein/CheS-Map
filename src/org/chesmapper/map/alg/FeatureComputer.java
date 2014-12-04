package org.chesmapper.map.alg;

import java.util.List;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;

public interface FeatureComputer
{
	public void computeFeatures(DatasetFile dataset);

	public List<CompoundProperty> getFeatures();

	public List<CompoundProperty> getProperties();

	public List<CompoundData> getCompounds();

	public int getNumFeatureSets();
}
