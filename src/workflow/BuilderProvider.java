package workflow;

import main.Settings;
import alg.Algorithm;
import alg.build3d.ThreeDBuilder;

public class BuilderProvider extends AbstractAlgorithmProvider
{
	@Override
	public Algorithm[] getAlgorithms()
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
}
