package alg.build3d;

import gui.binloc.Binary;
import main.Settings;
import util.ExternalToolUtil;
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
		ExternalToolUtil.run("obgen3d", new String[] { Settings.BABEL_BINARY.getLocation(), "--gen3d", "-d", "-isdf",
				datasetFile.getSDFPath(false), "-osdf", outfile });
	}

	@Override
	public Binary getBinary()
	{
		return Settings.BABEL_BINARY;
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
