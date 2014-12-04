package org.chesmapper.map.workflow;

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.alg.build3d.ThreeDBuilder;
import org.chesmapper.map.main.Settings;

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
