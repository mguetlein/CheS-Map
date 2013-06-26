package workflow;

import java.util.HashMap;
import java.util.Properties;

import dataInterface.CompoundPropertySet;

public interface FeatureMappingWorkflowProvider extends MappingWorkflowProvider
{
	public CompoundPropertySet[] getFeaturesFromMappingWorkflow(Properties mappingWorkflowProps, boolean storeToSettings);

	public void exportFeaturesToMappingWorkflow(HashMap<String, CompoundPropertySet[]> features, Properties props);
}
