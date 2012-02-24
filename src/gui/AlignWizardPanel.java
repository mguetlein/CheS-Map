package gui;

import gui.wizard.GenericWizardPanel;
import main.Settings;
import alg.Algorithm;
import alg.align3d.ThreeDAligner;

public class AlignWizardPanel extends GenericWizardPanel
{
	boolean canProceed = false;

	public AlignWizardPanel(CheSMapperWizard w)
	{
		super(w);
	}

	@Override
	protected Algorithm[] getAlgorithms()
	{
		return ThreeDAligner.ALIGNER;
	}

	@Override
	public String getTitle()
	{
		return Settings.text("align.title");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("align.desc");
	}

	public ThreeDAligner getAlginer()
	{
		return (ThreeDAligner) getSelectedAlgorithm();
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
