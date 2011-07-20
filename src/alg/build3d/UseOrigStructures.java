package alg.build3d;

import gui.Progressable;
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
		f = dataset.getSDFPath();
	}

	@Override
	public String getName()
	{
		return "Use Original Structures";
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
	public String getPreconditionErrors()
	{
		return null;
	}

}
