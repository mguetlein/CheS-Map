package alg.build3d;

import alg.Algorithm;
import data.DatasetFile;

public interface ThreeDBuilder extends Algorithm
{
	public boolean isCached(DatasetFile datasetFile);

	public void build3D(DatasetFile datasetFile);

	public String get3DSDFFile();

	public boolean isReal3DBuilder();
}
