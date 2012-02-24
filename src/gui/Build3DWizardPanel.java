package gui;

import gui.wizard.GenericWizardPanel;
import main.Settings;
import alg.build3d.ThreeDBuilder;

public class Build3DWizardPanel extends GenericWizardPanel
{
	public Build3DWizardPanel(CheSMapperWizard w)
	{
		super(w);
	}

	@Override
	protected ThreeDBuilder[] getAlgorithms()
	{
		return ThreeDBuilder.BUILDERS;
	}

	@Override
	public String getTitle()
	{
		return Settings.text("build3d.title");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("build3d.desc");
	}

	public ThreeDBuilder get3DBuilder()
	{
		return (ThreeDBuilder) getSelectedAlgorithm();
	}

	@Override
	protected boolean hasSimpleView()
	{
		return false;
	}

	@Override
	protected SimplePanel createSimpleView()
	{
		return null;
	}

}
