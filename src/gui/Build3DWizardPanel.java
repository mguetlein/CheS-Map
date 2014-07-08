package gui;

import gui.wizard.ListWizardPanel;
import workflow.BuilderProvider;
import alg.build3d.ThreeDBuilder;

public class Build3DWizardPanel extends ListWizardPanel
{
	public Build3DWizardPanel(CheSMapperWizard w)
	{
		super(w, new BuilderProvider());
	}

	public ThreeDBuilder get3DBuilder()
	{
		return (ThreeDBuilder) getSelectedAlgorithm();
	}
}
