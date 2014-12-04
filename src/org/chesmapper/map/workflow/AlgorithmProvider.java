package org.chesmapper.map.workflow;

import java.util.Properties;

import org.chesmapper.map.alg.Algorithm;

public interface AlgorithmProvider extends MappingWorkflowProvider
{
	public Algorithm[] getAlgorithms();

	/**
	 * @return index within getAlgorithms which should be default
	 */
	public int getDefaultListSelection();

	/**
	 * title for gui (also used to store props)
	 */
	public String getTitle();

	/**
	 * description for gui
	 */
	public String getDescription();

	/**
	 * to de-serialize stored algorithms
	 */
	public Algorithm getAlgorithmByName(String algorithmName);

	/**
	 * loads stored algorithm from global-props (this might be ignored if simple-view-algorithm provider)
	 */
	public Algorithm getListAlgorithmFromProps();

	/**
	 * stores algorithm to global props
	 */
	public void storeListAlgorithmToProps(Algorithm algorithm);

	/**
	 * stores algorithm with its properties to the prop-workflow
	 */
	public void exportAlgorithmToMappingWorkflow(Algorithm algorithm, Properties mappingWorkflowProps);

	/**
	 * loads algorithm from workflow-props (storeToSettings sets global-props accordingly, to have the correct settings available when starting the wizard)
	 */
	public Algorithm getAlgorithmFromMappingWorkflow(Properties mappingWorkflowProps, boolean storeToGlobalSettings);

}
