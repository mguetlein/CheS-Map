package org.chesmapper.map.alg.build3d;

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.data.DatasetFile;

public interface ThreeDBuilder extends Algorithm
{
	public static final ThreeDBuilder BUILDERS[] = { UseOrigStructures.INSTANCE, CDK3DBuilder.INSTANCE,
			OpenBabel3DBuilder.INSTANCE };//, SDFImport3DBuilder.INSTANCE };

	public boolean isCached(DatasetFile datasetFile);

	public void build3D(DatasetFile datasetFile) throws Exception;

	public String get3DSDFile();

	public boolean isReal3DBuilder();
}
