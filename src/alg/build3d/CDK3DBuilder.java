package alg.build3d;

import gui.property.Property;
import gui.property.SelectProperty;
import main.Settings;
import data.DatasetFile;
import data.FeatureService;

public class CDK3DBuilder extends AbstractReal3DBuilder
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
		return Settings.text("build3d.cdk");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("build3d.cdk.desc", Settings.CDK_STRING);
	}

	@Override
	public String getInitials()
	{
		return "cdk_" + forcefield + "_";
	}

}
