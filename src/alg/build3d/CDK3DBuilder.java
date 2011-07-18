package alg.build3d;

import gui.property.Property;
import main.Settings;
import data.CDKService;

public class CDK3DBuilder extends Abstract3DBuilder
{
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
	public void build3D(String sdfFile, String outfile)
	{
		CDKService.generate3D(sdfFile, outfile);
	}

	@Override
	public String getName()
	{
		return "CDK 3D-Builder";
	}

	@Override
	public String getDescription()
	{
		return "Employs " + Settings.CDK_STRING + ".\n\n"
				+ "Employs the Model Builder 3D from CDK. Tends to be faster but less acurate then OpenBabel.";
	}

	@Override
	public String getInitials()
	{
		return "cdk";
	}

}
