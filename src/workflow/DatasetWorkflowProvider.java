package workflow;

import java.util.Properties;

import data.DatasetFile;

public interface DatasetWorkflowProvider extends WorkflowProvider
{
	public void exportDatasetToWorkflow(String datasetPath, Properties props);

	public DatasetFile getDatasetFromWorkflow(Properties props, boolean storeToSettings, String alternateDatasetDir);
}
