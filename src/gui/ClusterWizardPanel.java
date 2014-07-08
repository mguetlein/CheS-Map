package gui;

import gui.wizard.SimpleViewWizardPanel;
import workflow.ClustererProvider;
import alg.cluster.DatasetClusterer;

public class ClusterWizardPanel extends SimpleViewWizardPanel
{
	public ClusterWizardPanel(CheSMapperWizard w)
	{
		super(w, new ClustererProvider());
	}

	public DatasetClusterer getDatasetClusterer()
	{
		return (DatasetClusterer) getSelectedAlgorithm();
	}

	@Override
	public String getSimpleQuestion()
	{
		return "Cluster dataset?";
	}
}
