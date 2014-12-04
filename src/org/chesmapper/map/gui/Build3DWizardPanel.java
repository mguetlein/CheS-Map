package org.chesmapper.map.gui;

import org.chesmapper.map.alg.build3d.ThreeDBuilder;
import org.chesmapper.map.gui.wizard.ListWizardPanel;
import org.chesmapper.map.workflow.BuilderProvider;

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
