package workflow;

import main.Settings;
import alg.Algorithm;
import alg.align3d.ThreeDAligner;

public class AlignerProvider extends AbstractAlgorithmProvider
{
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

	@Override
	public Algorithm[] getAlgorithms()
	{
		return ThreeDAligner.ALIGNER;
	}
}
