package workflow;

import java.util.Properties;

import alg.Algorithm;

public interface AlgorithmWorkflowProvider extends WorkflowProvider
{
	public void exportAlgorithmToWorkflow(Algorithm algorithm, Properties props);

	public Algorithm getAlgorithmFromWorkflow(Properties props, boolean storeToSettings);

}
