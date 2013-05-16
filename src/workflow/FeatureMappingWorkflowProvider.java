package workflow;

import java.util.Properties;

import dataInterface.MoleculePropertySet;

public interface FeatureMappingWorkflowProvider extends MappingWorkflowProvider
{
	public MoleculePropertySet[] getFeaturesFromMappingWorkflow(Properties mappingWorkflowProps, boolean storeToSettings);

	public void exportFeaturesToMappingWorkflow(String featureNames[], boolean selectAllInternalFeatures,
			Properties props);
}
