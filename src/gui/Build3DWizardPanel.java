package gui;

import alg.build3d.CDK3DBuilder;
import alg.build3d.OpenBabel3DBuilder;
import alg.build3d.ThreeDBuilder;
import alg.build3d.UseOrigStructures;
import data.DatasetFile;

public class Build3DWizardPanel extends GenericWizardPanel
{
	public Build3DWizardPanel(CheSMapperWizard w)
	{
		super(w);
	}

	public static final ThreeDBuilder BUILDERS[] = { new UseOrigStructures(), new CDK3DBuilder(),
			new OpenBabel3DBuilder() };

	@Override
	protected ThreeDBuilder[] getAlgorithms()
	{
		return BUILDERS;
	}

	@Override
	public String getTitle()
	{
		return "Create 3D Structures";
	}

	@Override
	public String getAlgorithmType()
	{
		return "Create 3D Structure Algorithms";
	}

	@Override
	public String getDescription()
	{
		return "Compute 3D coordinates for all compounds in the dataset.";
	}

	public void update(DatasetFile dataset)
	{
		//		if (!preconditionsMet)
		//			return;
		ThreeDBuilder alg = get3DBuilder();
		if (alg.isReal3DBuilder() && alg.threeDFileAlreadyExists(dataset))
			setInfo("3D structures already built with '" + alg.getName()
					+ "' (result was cached, no time consuming recomputation needed)", MsgType.INFO);
		else if (dataset.has3D() && alg.isReal3DBuilder())
			setInfo("3D already available in original dataset '" + dataset.getName()
					+ "' (at least one Z-Coordinate is set), this will override the orig 3D structure", MsgType.WARNING);
		else if (!dataset.has3D() && !alg.isReal3DBuilder())
			setInfo("3D is NOT available in original dataset '" + dataset.getName() + "' (all Z-Coordinates are 0)",
					MsgType.WARNING);
	}

	public ThreeDBuilder get3DBuilder()
	{
		return (ThreeDBuilder) getSelectedAlgorithm();
	}

}
