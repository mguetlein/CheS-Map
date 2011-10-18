package alg.build3d;

import gui.binloc.Binary;
import gui.property.Property;
import gui.property.SelectProperty;
import main.Settings;
import data.DatasetFile;
import data.FeatureService;

public class CDK3DBuilder extends Abstract3DBuilder
{
	public static final String[] FORCEFIELDS = { "mm2", "mmff94" };
	SelectProperty forcefield = new SelectProperty("forcefield", FORCEFIELDS, FORCEFIELDS[0]);

	@Override
	public Property[] getProperties()
	{
		return new Property[] { forcefield };
	}

	@Override
	public void build3D(DatasetFile dataset, String outfile)
	{
		FeatureService.generateCDK3D(dataset, outfile, forcefield.getValue().toString());
	}

	@Override
	public String getName()
	{
		return "CDK 3D Structure Generation";
	}

	@Override
	public String getDescription()
	{
		return "Uses "
				+ Settings.CDK_STRING
				+ ".\n"
				+ "The Model Builder 3D supports 2 different forcefields. Tends to be faster but less acurate then OpenBabel.\n\nCDK API: http://pele.farmbio.uu.se/nightly/api/org/openscience/cdk/modeling/builder3d/ModelBuilder3D.html";
	}

	@Override
	public String getInitials()
	{
		return "cdk_" + forcefield + "_";
	}

	@Override
	public Binary getBinary()
	{
		return null;
	}

	@Override
	public String getWarning()
	{
		return null;
	}

}
