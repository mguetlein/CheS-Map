package alg.build3d;

import main.Settings;
import data.DatasetFile;

public class UseOrigStructures extends Abstract3DBuilder
{
	public static final UseOrigStructures INSTANCE = new UseOrigStructures();

	private UseOrigStructures()
	{
	}

	String f;

	@Override
	public String get3DSDFFile()
	{
		return f;
	}

	@Override
	public void build3D(DatasetFile dataset)
	{
		f = dataset.getSDFPath(false);
	}

	@Override
	public String getName()
	{
		return Settings.text("build3d.no-3d");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("build3d.no-3d.desc");
	}

	@Override
	public boolean isReal3DBuilder()
	{
		return false;
	}

	@Override
	public boolean isCached(DatasetFile datasetFile)
	{
		return true;
	}

}
