package gui;

import gui.property.PropertyPanel;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import alg.Algorithm;
import alg.embed3d.AbstractRTo3DEmbedder;
import alg.embed3d.Random3DEmbedder;
import alg.embed3d.ThreeDEmbedder;
import alg.embed3d.WekaPCA3DEmbedder;
import data.DatasetFile;
import dataInterface.MoleculeProperty.Type;

public class EmbedWizardPanel extends GenericWizardPanel
{
	public static final ThreeDEmbedder EMBEDDERS[] = { new Random3DEmbedder(), new WekaPCA3DEmbedder(),
			new AbstractRTo3DEmbedder.PCAFeature3DEmbedder(),
			new AbstractRTo3DEmbedder.TSNEFeature3DEmbedder(), new AbstractRTo3DEmbedder.SMACOF3DEmbedder() };

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
	public void update(DatasetFile dataset, int numFeatures, Type featureType)
	{
		if (!preconditionsMet)
			return;
		canProceed = !get3DEmbedder().requiresFeatures() || numFeatures > 0;
		if (!canProceed)
			setInfo(get3DEmbedder().getName()
					+ " requires features, but no features are selected.\nPlease select features in step 3., or select another embedding method.",
					MsgType.ERROR);
		else
		{
			if (getSelectedAlgorithm().getWarning() != null)
				setInfo(getSelectedAlgorithm().getWarning(), MsgType.WARNING);
			else if (get3DEmbedder().requiresFeatures() && featureType == Type.NOMINAL)
				setInfo("Only nominal features selected. Sometimes, this will result in a lot of compounds with equal feature values (especially inside clusters) that cannot be distinguished while embedding.",
						MsgType.INFO);
			else
				setInfo("", MsgType.EMPTY);
		}
	}

	protected int defaultSelection()
	{
		return 1;
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
		return "Embed into 3D Space"; // clusters and compounds";
	}

	@Override
	public String getAlgorithmType()
	{
		return "Embed Algorithms";
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
