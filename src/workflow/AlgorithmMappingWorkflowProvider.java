package workflow;

import java.util.Properties;

import alg.Algorithm;

public interface AlgorithmMappingWorkflowProvider extends MappingWorkflowProvider
{
	public void exportAlgorithmToMappingWorkflow(Algorithm algorithm, Properties mappingWorkflowProps);

	public Algorithm getAlgorithmFromMappingWorkflow(Properties mappingWorkflowProps, boolean storeToSettings);

}
