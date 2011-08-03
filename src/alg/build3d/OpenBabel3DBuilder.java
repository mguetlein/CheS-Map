package alg.build3d;

import gui.property.Property;
import main.Settings;
import util.ExternalToolUtil;
import data.DatasetFile;

public class OpenBabel3DBuilder extends Abstract3DBuilder
{
	@Override
	public void build3D(DatasetFile datasetFile, String outfile)
	{
		ExternalToolUtil.run("obgen3d", Settings.CV_BABEL_PATH + " --gen3d -d -isdf " + datasetFile.getSDFPath(false)
				+ " -osdf " + outfile);
	}

	@Override
	public String getPreconditionErrors()
	{
		if (Settings.CV_BABEL_PATH != null)
			return null;
		else
			return "OpenBabel command 'babel' could not be found";
	}

	@Override
	public String getDescription()
	{
		return "Uses the openbabel gen3d option to compute 3D coordinates. May take very long, up to a view minutes per compound. The result is stored in an sdf file that can be used next time.";
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
		return "OpenBabel 3D-Builder";
	}

	@Override
	public String getInitials()
	{
		return "ob";
	}
}
