package workflow;

import java.util.Properties;

import dataInterface.MoleculePropertySet;

public interface FeatureWorkflowProvider extends WorkflowProvider
{
	public MoleculePropertySet[] getFeaturesFromWorkflow(Properties props, boolean storeToSettings);

	public void exportFeaturesToWorkflow(String featureNames[], Properties props);
}
