package alg.build3d;

import gui.Progressable;
import gui.binloc.Binary;
import gui.property.Property;
import data.DatasetFile;

public class UseOrigStructures implements ThreeDBuilder
{
	String f;

	@Override
	public String get3DSDFFile()
	{
		return f;
	}

	@Override
	public void build3D(DatasetFile dataset, Progressable progress)
	{
		f = dataset.getSDFPath(false);
	}

	@Override
	public String getName()
	{
		return "No 3D-Structure Generation (Use Original Structures)";
	}

	@Override
	public String getDescription()
	{
		return "Does NOT create 3D structures, leaves the dataset as it is.";
	}

	@Override
	public Property[] getProperties()
	{
		return null;
	}

	@Override
	public void setProperties(Property[] properties)
	{
	}

	@Override
	public boolean isReal3DBuilder()
	{
		return false;
	}

	@Override
	public Binary getBinary()
	{
		return null;
	}

}
