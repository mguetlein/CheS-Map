package alg.embed3d;

import java.util.List;

import javax.vecmath.Vector3f;

import alg.Algorithm;
import alg.DistanceMeasure;
import alg.embed3d.r.PCAFeature3DEmbedder;
import alg.embed3d.r.SMACOF3DEmbedder;
import alg.embed3d.r.Sammon3DEmbedder;
import alg.embed3d.r.TSNEFeature3DEmbedder;
import data.DatasetFile;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;

public interface ThreeDEmbedder extends Algorithm
{
	public static final ThreeDEmbedder EMBEDDERS[] = { Random3DEmbedder.INSTANCE, WekaPCA3DEmbedder.INSTANCE,
			PCAFeature3DEmbedder.INSTANCE, Sammon3DEmbedder.INSTANCE, SMACOF3DEmbedder.INSTANCE,
			TSNEFeature3DEmbedder.INSTANCE };

	public boolean requiresFeatures();

	public void embedDataset(DatasetFile dataset, List<CompoundData> instances, List<CompoundProperty> features)
			throws Exception;

	public List<Vector3f> getPositions();

	public double getRSquare();

	public double getCCC();

	//	public CompoundPropertyEmbedQuality getEmbedQuality(CompoundProperty p, DatasetFile dataset,
	//			List<MolecularPropertyOwner> instances);

	public boolean isLinear();

	public boolean isLocalMapping();

	public CompoundProperty getCCCProperty();

	public DistanceMeasure getDistanceMeasure();

}
