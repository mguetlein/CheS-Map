package workflow;

import java.util.Properties;

import data.DatasetFile;
import dataInterface.CompoundPropertySet;

public interface FeatureMappingWorkflowProvider extends MappingWorkflowProvider
{
	public CompoundPropertySet[] getFeaturesFromMappingWorkflow(Properties mappingWorkflowProps,
			boolean storeToSettings, DatasetFile dataset);

	public void exportFeaturesToMappingWorkflow(CompoundPropertySet[] features, Properties props, DatasetFile dataset);
}
