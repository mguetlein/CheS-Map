package gui;

import gui.property.PropertyPanel;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import main.Settings;
import alg.Algorithm;
import alg.embed3d.AbstractRTo3DEmbedder;
import alg.embed3d.Random3DEmbedder;
import alg.embed3d.ThreeDEmbedder;
import alg.embed3d.WekaPCA3DEmbedder;

public class EmbedWizardPanel extends GenericWizardPanel
{
	public static final ThreeDEmbedder EMBEDDERS[] = { new Random3DEmbedder(), new WekaPCA3DEmbedder(),
			new AbstractRTo3DEmbedder.PCAFeature3DEmbedder(), new AbstractRTo3DEmbedder.TSNEFeature3DEmbedder(),
			new AbstractRTo3DEmbedder.SMACOF3DEmbedder() };

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

	protected int defaultSelection()
	{
		return 1;
	}

	public ThreeDEmbedder get3DEmbedder()
	{
		return (ThreeDEmbedder) getSelectedAlgorithm();
	}

	@Override
	public String getTitle()
	{
		return Settings.text("embed.title");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("embed.desc");
	}

	@Override
	protected Algorithm[] getAlgorithms()
	{
		return EMBEDDERS;
	}
}
