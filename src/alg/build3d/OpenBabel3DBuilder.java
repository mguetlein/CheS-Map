package alg.build3d;

import gui.binloc.Binary;
import main.BinHandler;
import main.Settings;
import main.TaskProvider;
import babel.OBWrapper;
import babel.OBWrapper.Aborter;
import data.DatasetFile;

public class OpenBabel3DBuilder extends AbstractReal3DBuilder
{
	public static final OpenBabel3DBuilder INSTANCE = new OpenBabel3DBuilder();

	private OpenBabel3DBuilder()
	{
	}

	@Override
	public void build3D(DatasetFile datasetFile, String outfile)
	{
		Aborter aborter = new OBWrapper.Aborter()
		{
			@Override
			public boolean abort()
			{
				return !TaskProvider.isRunning();
			}
		};
		if (datasetFile.getLocalPath() != null && datasetFile.getLocalPath().endsWith(".smi"))
			BinHandler.getOBWrapper().compute3DfromSmiles(Settings.BABEL_3D_CACHE, datasetFile.getLocalPath(), outfile,
					aborter);
		else
			BinHandler.getOBWrapper().compute3DfromSDF(Settings.BABEL_3D_CACHE, datasetFile.getSDFPath(false), outfile,
					aborter);
	}

	@Override
	public Binary getBinary()
	{
		return BinHandler.BABEL_BINARY;
	}

	@Override
	public String getName()
	{
		return Settings.text("build3d.openbabel");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("build3d.openbabel.desc", Settings.OPENBABEL_STRING);
	}

	@Override
	public String getInitials()
	{
		return "ob";
	}
}
