package alg.embed3d;

import java.util.List;

import javax.vecmath.Vector3f;

import util.DistanceMatrix;
import alg.Algorithm;
import data.DatasetFile;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

public interface ThreeDEmbedder extends Algorithm
{
	public boolean requiresFeatures();

	public boolean requiresDistances();

	public void embed(DatasetFile dataset, List<MolecularPropertyOwner> instances, List<MoleculeProperty> features,
			DistanceMatrix<MolecularPropertyOwner> distances);

	public List<Vector3f> getPositions();

}
