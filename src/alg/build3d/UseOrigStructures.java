package alg.build3d;

import gui.Progressable;
import gui.property.Property;

public class UseOrigStructures implements ThreeDBuilder
{
	String f;

	@Override
	public String get3DFile()
	{
		return f;
	}

	@Override
	public void build3D(String sdfFile, Progressable progress)
	{
		f = sdfFile;
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
