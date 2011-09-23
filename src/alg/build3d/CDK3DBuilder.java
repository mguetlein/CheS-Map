package alg.build3d;

import gui.binloc.Binary;
import gui.property.Property;
import gui.property.SelectProperty;
import main.Settings;

import org.openscience.cdk.modeling.builder3d.ModelBuilder3D;

import data.DatasetFile;
import data.FeatureService;

public class CDK3DBuilder extends Abstract3DBuilder
{
	public static final String[] FORCEFIELDS = { "mm2", "mmff94" };
	private String forcefield = FORCEFIELDS[0];
	private final String forcefieldDefault = FORCEFIELDS[0];

	public static final String PROPERTY_FORCEFIELD = "forcefield";
	private Property[] properties = new Property[] { new SelectProperty(PROPERTY_FORCEFIELD, FORCEFIELDS, forcefield,
			forcefieldDefault) };

	@Override
	public Property[] getProperties()
	{
		return properties;
	}

	@Override
	public void setProperties(Property[] properties)
	{
		for (Property property : properties)
		{
			if (property.getName().equals(PROPERTY_FORCEFIELD))
				forcefield = property.getValue().toString();
		}
	}

	@Override
	public void build3D(DatasetFile dataset, String outfile)
	{
		FeatureService.generateCDK3D(dataset, outfile, forcefield);
	}

	@Override
	public String getName()
	{
		return "CDK 3D Structure Generation";
	}

	@Override
	public String getDescription()
	{
		return "Uses " + Settings.CDK_STRING + ".\n\n" + "The Model Builder 3D (" + ModelBuilder3D.class.getName()
				+ ") supports 2 different forcefields. Tends to be faster but less acurate then OpenBabel.";
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
