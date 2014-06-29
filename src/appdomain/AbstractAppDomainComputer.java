package appdomain;

import gui.FeatureWizardPanel.FeatureInfo;
import gui.Messages;
import gui.binloc.Binary;
import gui.property.Property;

import java.util.List;

import main.Settings;
import util.ArrayUtil;
import alg.cluster.DatasetClusterer;
import data.DatasetFile;
import dataInterface.DefaultCompoundProperty;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;

public abstract class AbstractAppDomainComputer implements AppDomainComputer
{
	@Override
	public String toString()
	{
		return getName();
	}

	@Override
	public String getDescription()
	{
		return Settings.text("props.app-domain." + getShortName() + ".desc");
	}

	public abstract String getShortName();

	protected double pValues[];
	protected boolean inside[];

	//	protected DatasetFile dataset;
	protected List<CompoundData> compounds;
	protected List<CompoundProperty> features;

	public abstract void computeAppDomain();

	public void computeAppDomain(//DatasetFile dataset, 
			List<CompoundData> compounds, List<CompoundProperty> features, double[][] featureDistanceMatrix)
	{
		//		this.dataset = dataset;
		this.compounds = compounds;
		this.features = features;
		pValues = new double[compounds.size()];
		inside = new boolean[compounds.size()];
		computeAppDomain();
	}

	@Override
	public CompoundProperty getInsideAppDomainProperty()
	{
		//		return AppDomainPropertySet.create(getShortName(), dataset, inside,
		//				dataset.getAppDomainValuesFilePath(this, getShortName() + "inside"));
		return new DefaultCompoundProperty(getShortName() + "-inside", ".", ArrayUtil.toStringArray(ArrayUtil
				.toBooleanArray(inside)));
	}

	@Override
	public CompoundProperty getPropabilityAppDomainProperty()
	{
		//		return AppDomainPropertySet.create(getShortName(), dataset, pValues,
		//				dataset.getAppDomainValuesFilePath(this, getShortName() + "propability"));
		return new DefaultCompoundProperty(getShortName() + "-propability", ".", ArrayUtil.toDoubleArray(pValues));
	}

	@Override
	public String getName()
	{
		return Settings.text("props.app-domain." + getShortName());
	}

	@Override
	public Property[] getProperties()
	{
		return null;
	}

	@Override
	public Binary getBinary()
	{
		return null;
	}

	@Override
	public Messages getMessages(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		return null;
	}

	@Override
	public Messages getProcessMessages()
	{
		return null;
	}

	@Override
	public Property getRandomSeedProperty()
	{
		return null;
	}

	@Override
	public Property getRandomRestartProperty()
	{
		return null;
	}

	@Override
	public void update(DatasetFile dataset)
	{
	}

}
