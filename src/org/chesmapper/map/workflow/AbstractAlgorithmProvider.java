package org.chesmapper.map.workflow;

import java.util.Properties;

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.main.PropHandler;
import org.mg.javalib.gui.property.Property;

public abstract class AbstractAlgorithmProvider implements AlgorithmProvider
{
	protected String getPropKeyMethod()
	{
		return getTitle() + "-method";
	}

	@Override
	public Algorithm getAlgorithmByName(String algorithmName)
	{
		for (Algorithm a : getAlgorithms())
			if (a.getName().equals(algorithmName))
				return a;
		return null;
	}

	@Override
	public int getDefaultListSelection()
	{
		return -1;
	}

	@Override
	public void storeListAlgorithmToProps(Algorithm algorithm)
	{
		storeListAlgorithmToProps(algorithm, PropHandler.getProperties());
	}

	private void storeListAlgorithmToProps(Algorithm algorithm, Properties props)
	{
		props.put(getPropKeyMethod(), algorithm.getName());
		if (algorithm.getProperties() != null)
			for (Property p : algorithm.getProperties())
				p.put(props);
	}

	@Override
	public Algorithm getListAlgorithmFromProps()
	{
		return getListAlgorithmFromProps(PropHandler.getProperties(), false);
	}

	private Algorithm getListAlgorithmFromProps(Properties props, boolean storeToGlobalSettings)
	{
		Algorithm alg = getAlgorithmByName((String) props.get(getPropKeyMethod()));
		if (alg == null)
			alg = getAlgorithms()[0];
		if (alg.getProperties() != null)
			for (Property p : alg.getProperties())
				p.loadOrResetToDefault(props);
		if (storeToGlobalSettings)
			storeListAlgorithmToProps(alg, PropHandler.getProperties());
		return alg;
	}

	@Override
	public void exportAlgorithmToMappingWorkflow(Algorithm algorithm, Properties mappingWorkflowProps)
	{
		storeListAlgorithmToProps(algorithm, mappingWorkflowProps);
	}

	@Override
	public Algorithm getAlgorithmFromMappingWorkflow(Properties mappingWorkflowProps, boolean storeToGlobalSettings)
	{
		return getListAlgorithmFromProps(mappingWorkflowProps, storeToGlobalSettings);
	}

	@Override
	public void exportSettingsToMappingWorkflow(Properties mappingWorkflowProps)
	{
		if (PropHandler.containsKey(getPropKeyMethod()))
			mappingWorkflowProps.put(getPropKeyMethod(), PropHandler.get(getPropKeyMethod()));
		Algorithm algorithm = getAlgorithmFromMappingWorkflow(PropHandler.getProperties(), false);
		if (algorithm.getProperties() != null)
			for (Property p : algorithm.getProperties())
				p.put(mappingWorkflowProps);
	}
}
