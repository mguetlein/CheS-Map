package alg.build3d;

import gui.Progressable;
import alg.Algorithm;
import data.DatasetFile;

public interface ThreeDBuilder extends Algorithm
{
	public void build3D(DatasetFile datasetFile, Progressable progress);

	public String get3DSDFFile();

	public String getDescription();

	public boolean isReal3DBuilder();

	public String getPreconditionErrors();
}
