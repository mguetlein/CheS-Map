package workflow;

import java.util.Properties;

import data.DatasetFile;

public interface DatasetMappingWorkflowProvider extends MappingWorkflowProvider
{
	public void exportDatasetToMappingWorkflow(String datasetPath, boolean bigDataMode, Properties mappingWorkflowProps);

	public DatasetFile getDatasetFromMappingWorkflow(Properties mappingWorkflowProps, boolean storeToSettings,
			String alternateDatasetDir);
}
