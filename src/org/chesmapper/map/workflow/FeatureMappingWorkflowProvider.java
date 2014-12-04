package org.chesmapper.map.workflow;

import java.util.Properties;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.CompoundPropertySet;

public interface FeatureMappingWorkflowProvider extends MappingWorkflowProvider
{
	public CompoundPropertySet[] getFeaturesFromMappingWorkflow(Properties mappingWorkflowProps,
			boolean storeToSettings, DatasetFile dataset);

	public void exportFeaturesToMappingWorkflow(CompoundPropertySet[] features, Properties props, DatasetFile dataset);
}
