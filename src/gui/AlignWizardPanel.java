package gui;

import main.Settings;
import alg.Algorithm;
import alg.align3d.MCSAligner;
import alg.align3d.MaxFragAligner;
import alg.align3d.NoAligner;
import alg.align3d.ThreeDAligner;

public class AlignWizardPanel extends GenericWizardPanel
{
	boolean canProceed = false;
	public static final ThreeDAligner ALIGNER[] = new ThreeDAligner[] { new NoAligner(), new MCSAligner(),
			new MaxFragAligner() };

	public AlignWizardPanel(CheSMapperWizard w)
	{
		super(w);
	}

	@Override
	protected Algorithm[] getAlgorithms()
	{
		return ALIGNER;
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
}
