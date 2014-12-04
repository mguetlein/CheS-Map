package org.chesmapper.map.workflow;

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.alg.align3d.ThreeDAligner;
import org.chesmapper.map.main.Settings;

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
