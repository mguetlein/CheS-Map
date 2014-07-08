package gui;

import gui.wizard.SimpleViewWizardPanel;
import workflow.EmbedderProvider;
import alg.embed3d.ThreeDEmbedder;

public class EmbedWizardPanel extends SimpleViewWizardPanel
{
	public EmbedWizardPanel(CheSMapperWizard w)
	{
		super(w, new EmbedderProvider());
	}

	public ThreeDEmbedder get3DEmbedder()
	{
		return (ThreeDEmbedder) getSelectedAlgorithm();
	}

	@Override
	public String getSimpleQuestion()
	{
		return "Embedd compounds according to their feature values into 3D space?";
	}

}
