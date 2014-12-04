package org.chesmapper.map.workflow;

import java.util.Properties;

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.main.PropHandler;
import org.mg.javalib.gui.property.Property;

public abstract class AbstractSimpleViewAlgorithmProvider extends AbstractAlgorithmProvider implements
		SimpleViewAlgorithmProvider
{
	private String getPropKeySimple()
	{
		return getTitle() + "-simple-selected";
	}

	private String getPropKeyYes()
	{
		return getTitle() + "-simple-yes";
	}

	@Override
	public void storeSimpleSelectionToProps(boolean simpleSelected, boolean yesSelected)
	{
		storeSimpleSelectionToProps(simpleSelected, yesSelected, PropHandler.getProperties());
	}

	private void storeSimpleSelectionToProps(boolean simpleSelected, boolean yesSelected, Properties props)
	{
		props.put(getPropKeySimple(), simpleSelected + "");
		props.put(getPropKeyYes(), yesSelected + "");
		Property[] aProps = yesSelected ? getYesAlgorithm().getProperties() : getNoAlgorithm().getProperties();
		if (aProps != null)
			for (Property p : aProps)
				p.put(props);
	}

	@Override
	public boolean isSimpleSelectedFromProps()
	{
		return isSimpleSelectedFromProps(PropHandler.getProperties(), false);
	}

	private boolean isSimpleSelectedFromProps(Properties props, boolean storeToGlobalSettings)
	{
		if (!props.containsKey(getPropKeySimple()))
			props.put(getPropKeySimple(), "true");
		String val = (String) props.get(getPropKeySimple());
		if (storeToGlobalSettings)
			PropHandler.put(getPropKeySimple(), val);
		return val.equals("true");
	}

	@Override
	public boolean isYesSelectedFromProps()
	{
		return isYesSelectedFromProps(PropHandler.getProperties(), false);
	}

	private boolean isYesSelectedFromProps(Properties mappingWorkflowProps, boolean storeToGlobalSettings)
	{
		if (!mappingWorkflowProps.containsKey(getPropKeyYes()))
			mappingWorkflowProps.put(getPropKeyYes(), isYesDefault() ? "true" : "false");
		String val = (String) mappingWorkflowProps.get(getPropKeyYes());
		if (storeToGlobalSettings)
			PropHandler.put(getPropKeyYes(), val);
		return val.equals("true");
	}

	@Override
	public void exportAlgorithmToMappingWorkflow(Algorithm algorithm, Properties mappingWorkflowProps)
	{
		if (algorithm == null || algorithm == getYesAlgorithm() || algorithm == getNoAlgorithm())
			storeSimpleSelectionToProps(true, algorithm == getYesAlgorithm(), mappingWorkflowProps);
		else
		{
			storeSimpleSelectionToProps(false, isYesDefault(), mappingWorkflowProps);
			super.exportAlgorithmToMappingWorkflow(algorithm, mappingWorkflowProps);
		}
	}

	@Override
	public Algorithm getAlgorithmFromMappingWorkflow(Properties mappingWorkflowProps, boolean storeToGlobalSettings)
	{
		if (isSimpleSelectedFromProps(mappingWorkflowProps, storeToGlobalSettings))
		{
			Algorithm alg;
			if (isYesSelectedFromProps(mappingWorkflowProps, storeToGlobalSettings))
				alg = getYesAlgorithm();
			else
				alg = getNoAlgorithm();
			if (alg.getProperties() != null)
				for (Property p : alg.getProperties())
					p.loadOrResetToDefault(mappingWorkflowProps);
			if (storeToGlobalSettings)
				if (alg.getProperties() != null)
					for (Property p : alg.getProperties())
						p.put(PropHandler.getProperties());
			return alg;
		}
		else
			return super.getAlgorithmFromMappingWorkflow(mappingWorkflowProps, storeToGlobalSettings);
	}

	@Override
	public void exportSettingsToMappingWorkflow(Properties mappingWorkflowProps)
	{
		if (PropHandler.containsKey(getPropKeySimple()))
			mappingWorkflowProps.put(getPropKeySimple(), PropHandler.get(getPropKeySimple()));
		if (PropHandler.containsKey(getPropKeyYes()))
			mappingWorkflowProps.put(getPropKeyYes(), PropHandler.get(getPropKeyYes()));
		Property[] aProps = isYesSelectedFromProps() ? getYesAlgorithm().getProperties() : getNoAlgorithm()
				.getProperties();
		if (aProps != null)
			for (Property p : aProps)
				p.put(mappingWorkflowProps);
		super.exportSettingsToMappingWorkflow(mappingWorkflowProps);
	}

}
