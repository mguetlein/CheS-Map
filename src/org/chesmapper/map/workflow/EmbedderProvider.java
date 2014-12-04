package org.chesmapper.map.workflow;

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.alg.embed3d.Random3DEmbedder;
import org.chesmapper.map.alg.embed3d.ThreeDEmbedder;
import org.chesmapper.map.alg.embed3d.WekaPCA3DEmbedder;
import org.chesmapper.map.main.Settings;

public class EmbedderProvider extends AbstractSimpleViewAlgorithmProvider
{
	public boolean isYesDefault()
	{
		return true;
	}

	@Override
	public ThreeDEmbedder getYesAlgorithm()
	{
		return WekaPCA3DEmbedder.INSTANCE_NO_PROBS;
	}

	@Override
	public ThreeDEmbedder getNoAlgorithm()
	{
		return Random3DEmbedder.INSTANCE;
	}

	@Override
	public Algorithm[] getAlgorithms()
	{
		return ThreeDEmbedder.EMBEDDERS;
	}

	@Override
	public int getDefaultListSelection()
	{
		return 1;
	}

	@Override
	public String getTitle()
	{
		return Settings.text("embed.title");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("embed.desc");
	}
}
