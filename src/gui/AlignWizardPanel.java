package gui;

import alg.Algorithm;
import alg.align3d.MCSAligner;
import alg.align3d.NoAligner;
import alg.align3d.ThreeDAligner;
import alg.cluster.DatasetClusterer;

public class AlignWizardPanel extends GenericWizardPanel
{
	boolean canProceed = false;
	public static final ThreeDAligner ALIGNER[] = new ThreeDAligner[] { new NoAligner(), new MCSAligner() };

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
		return "Align Compounds";
	}

	@Override
	public String getDescription()
	{
		return "Aligns the compounds inside a cluster with each other.";
	}

	public ThreeDAligner getAlginer()
	{
		return (ThreeDAligner) getSelectedAlgorithm();
	}

	public void update(DatasetClusterer clusterer)
	{
		if (!preconditionsMet)
			return;
		canProceed = true;
		//		canProceed = !getAlginer().isRealAligner() || (clusterer instanceof StructuralClusterer);
		//		if (!canProceed)
		//			setInfo(getAlginer().getClass().getSimpleName() + " requires structural features.", MsgType.ERROR);
		//		else
		//			setInfo("", MsgType.EMPTY);
	}

	@Override
	public boolean canProceed()
	{
		if (!preconditionsMet)
			return false;
		return canProceed;
	}

}
