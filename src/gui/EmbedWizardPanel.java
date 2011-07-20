package gui;

import gui.property.PropertyPanel;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import alg.Algorithm;
import alg.embed3d.AbstractRDistanceTo3DEmbedder;
import alg.embed3d.AbstractRFeatureTo3DEmbedder;
import alg.embed3d.Random3DEmbedder;
import alg.embed3d.ThreeDEmbedder;
import alg.embed3d.WekaPCA3DEmbedder;
import data.DatasetFile;

public class EmbedWizardPanel extends GenericWizardPanel
{
	public static final ThreeDEmbedder EMBEDDERS[] = { new WekaPCA3DEmbedder(), new Random3DEmbedder(),
			new AbstractRDistanceTo3DEmbedder.PCADistance3DEmbedder(),
			new AbstractRDistanceTo3DEmbedder.SMACOF3DEmbedder(),
			new AbstractRDistanceTo3DEmbedder.TSNEDistance3DEmbedder(),
			new AbstractRFeatureTo3DEmbedder.PCAFeature3DEmbedder(),
			new AbstractRFeatureTo3DEmbedder.TSNEFeature3DEmbedder(), };

	JRadioButton embedButtons[];
	ButtonGroup group;

	ThreeDEmbedder embedder;
	JPanel propertyPanel;
	PropertyPanel embedderPropertyPanel;
	boolean canProceed = false;

	public EmbedWizardPanel(CheSMapperWizard w)
	{
		super(w);
	}

	@Override
	public void update(DatasetFile dataset, int numNumericFeatures)
	{
		if (!preconditionsMet)
			return;
		canProceed = !get3DEmbedder().requiresNumericalFeatures() || numNumericFeatures > 0;
		if (!canProceed)
			setInfo(get3DEmbedder().getName()
					+ " requires numerical features, you have no features selected.\nPlease select numerical features in step 3., or select another embedding method.",
					MsgType.ERROR);
		else
			setInfo("", MsgType.EMPTY);
	}

	@Override
	public boolean canProceed()
	{
		if (!preconditionsMet)
			return false;
		return canProceed;
	}

	@Override
	public String getTitle()
	{
		return "3D Embedding"; // clusters and compounds";
	}

	public ThreeDEmbedder get3DEmbedder()
	{
		return (ThreeDEmbedder) getSelectedAlgorithm();
	}

	@Override
	public String getDescription()
	{
		return "Arranges the compounds and clusters in 3D space. The distance between clusters/compounds reflects their similarity";
	}

	@Override
	protected Algorithm[] getAlgorithms()
	{
		return EMBEDDERS;
	}
}
