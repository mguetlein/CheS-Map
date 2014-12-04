package org.chesmapper.map.alg.build3d;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.FeatureService;
import org.chesmapper.map.main.Settings;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.gui.property.SelectProperty;

public class CDK3DBuilder extends AbstractReal3DBuilder
{
	public static final String[] FORCEFIELDS = { "mm2", "mmff94" };
	public static final CDK3DBuilder INSTANCE = new CDK3DBuilder();

	private CDK3DBuilder()
	{
	}

	SelectProperty forcefield = new SelectProperty("forcefield", FORCEFIELDS, FORCEFIELDS[0]);

	@Override
	public Property[] getProperties()
	{
		return new Property[] { forcefield };
	}

	@Override
	public boolean[] build3D(DatasetFile dataset, String outfile)
	{
		return FeatureService.generateCDK3D(dataset, outfile, forcefield.getValue().toString());
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
}
