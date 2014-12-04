package org.chesmapper.map.alg.build3d;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.desc.DescriptorForMixturesHandler;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.mg.javalib.babel.OBWrapper;
import org.mg.javalib.babel.OBWrapper.Aborter;
import org.mg.javalib.gui.binloc.Binary;

public class OpenBabel3DBuilder extends AbstractReal3DBuilder
{
	public static final OpenBabel3DBuilder INSTANCE = new OpenBabel3DBuilder();

	private OpenBabel3DBuilder()
	{
	}

	@Override
	public boolean[] build3D(DatasetFile datasetFile, String outfile)
	{
		Aborter aborter = new OBWrapper.Aborter()
		{
			@Override
			public boolean abort()
			{
				return !TaskProvider.isRunning();
			}
		};
		if (datasetFile.getLocalPath() != null && datasetFile.getLocalPath().toLowerCase().endsWith(".smi"))
			return BinHandler.getOBWrapper().compute3DfromSmiles(Settings.BABEL_3D_CACHE, datasetFile.getLocalPath(),
					outfile, aborter);
		else if (datasetFile.getLocalPath() != null && datasetFile.getLocalPath().toLowerCase().endsWith(".csv"))
			return BinHandler.getOBWrapper().compute3DfromSmiles(Settings.BABEL_3D_CACHE, datasetFile.getSmiles(),
					outfile, aborter);
		else
		{
			boolean isMixture[] = new boolean[datasetFile.getCompounds().length];
			for (int i = 0; i < isMixture.length; i++)
				isMixture[i] = DescriptorForMixturesHandler.isMixture(datasetFile.getCompounds()[i]);
			return BinHandler.getOBWrapper().compute3DfromSDF(Settings.BABEL_3D_CACHE, datasetFile.getSDF(), isMixture,
					outfile, aborter);
		}
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
}
