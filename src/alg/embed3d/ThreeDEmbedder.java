package alg.embed3d;

import java.util.List;

import javax.vecmath.Vector3f;

import util.DistanceMatrix;
import alg.Algorithm;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

public interface ThreeDEmbedder extends Algorithm
{
	public boolean requiresNumericalFeatures();

	public boolean requiresDistances();

	public void embed(String filename, List<MolecularPropertyOwner> instances, List<MoleculeProperty> features,
			DistanceMatrix<MolecularPropertyOwner> distances);

	public List<Vector3f> getPositions();

}
