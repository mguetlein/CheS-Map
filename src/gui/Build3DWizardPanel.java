package gui;

import alg.build3d.CDK3DBuilder;
import alg.build3d.OpenBabel3DBuilder;
import alg.build3d.ThreeDBuilder;
import alg.build3d.UseOrigStructures;
import data.CDKService;

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
		return "Create 3D Structure";
	}

	@Override
	public String getDescription()
	{
		return "Compute 3D coordinates for all compounds in the dataset.";
	}

	public void update(String datasetFile, String datasetName)
	{
		if (!preconditionsMet)
			return;
		if (CDKService.has3D(datasetFile))
			setInfo("3D already available in original dataset '" + datasetName + "' (at least one Z-Coordinate is set)",
					(get3DBuilder().isReal3DBuilder() ? MsgType.WARNING : MsgType.INFO));
		else
			setInfo("3D is NOT available in original dataset '" + datasetName + "' (all Z-Coordinates are 0)",
					(get3DBuilder().isReal3DBuilder() ? MsgType.INFO : MsgType.WARNING));
	}

	public ThreeDBuilder get3DBuilder()
	{
		return (ThreeDBuilder) getSelectedAlgorithm();
	}

}
