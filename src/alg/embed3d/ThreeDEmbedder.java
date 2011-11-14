package alg.embed3d;

import java.util.List;

import javax.vecmath.Vector3f;

import alg.Algorithm;
import data.DatasetFile;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

public interface ThreeDEmbedder extends Algorithm
{
	public boolean requiresFeatures();

	public void embedDataset(DatasetFile dataset, List<MolecularPropertyOwner> instances, List<MoleculeProperty> features)
			throws Exception;

	public List<Vector3f> getPositions();

}
