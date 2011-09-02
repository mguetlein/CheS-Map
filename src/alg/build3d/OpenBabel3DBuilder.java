package alg.build3d;

import gui.binloc.Binary;
import gui.property.Property;
import main.Settings;
import util.ExternalToolUtil;
import data.DatasetFile;

public class OpenBabel3DBuilder extends Abstract3DBuilder
{
	@Override
	public void build3D(DatasetFile datasetFile, String outfile)
	{
		ExternalToolUtil.run("obgen3d",
				Settings.BABEL_BINARY.getLocation() + " --gen3d -d -isdf " + datasetFile.getSDFPath(false) + " -osdf "
						+ outfile);
	}

	@Override
	public Binary getBinary()
	{
		return Settings.BABEL_BINARY;
	}

	@Override
	public String getDescription()
	{
		return "Uses "
				+ Settings.OPENBABEL_STRING
				+ ".\n\n"
				+ "The 'gen3d' option is used to compute 3D coordinates. May take very long, up to a view minutes per compound. "
				+ "The result is stored so you have to do the computation only once. "
				+ "(More info: http://openbabel.org/wiki/Tutorial:Basic_Usage)";
	}

	@Override
	public Property[] getProperties()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setProperties(Property[] properties)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public String getName()
	{
		return "OpenBabel 3D Structure Generation";
	}

	@Override
	public String getInitials()
	{
		return "ob";
	}

	@Override
	public String getWarning()
	{
		return null;
	}
}
