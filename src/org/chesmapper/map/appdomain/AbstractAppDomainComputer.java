package org.chesmapper.map.appdomain;

import java.util.List;

import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.DefaultNominalProperty;
import org.chesmapper.map.dataInterface.DefaultNumericProperty;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.gui.FeatureWizardPanel.FeatureInfo;
import org.chesmapper.map.main.Settings;
import org.mg.javalib.gui.Messages;
import org.mg.javalib.gui.binloc.Binary;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.util.ArrayUtil;

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
	protected List<NumericProperty> features;

	public abstract void computeAppDomain();

	public void computeAppDomain(//DatasetFile dataset, 
			List<CompoundData> compounds, List<NumericProperty> features, double[][] featureDistanceMatrix)
	{
		//		this.dataset = dataset;
		this.compounds = compounds;
		this.features = features;
		pValues = new double[compounds.size()];
		inside = new boolean[compounds.size()];
		computeAppDomain();
	}

	@Override
	public NominalProperty getInsideAppDomainProperty()
	{
		//		return AppDomainPropertySet.create(getShortName(), dataset, inside,
		//				dataset.getAppDomainValuesFilePath(this, getShortName() + "inside"));
		return new DefaultNominalProperty(getShortName() + "-inside", ".", ArrayUtil.toStringArray(ArrayUtil
				.toBooleanArray(inside)));
	}

	@Override
	public NumericProperty getPropabilityAppDomainProperty()
	{
		//		return AppDomainPropertySet.create(getShortName(), dataset, pValues,
		//				dataset.getAppDomainValuesFilePath(this, getShortName() + "propability"));
		return new DefaultNumericProperty(getShortName() + "-propability", ".",
				ArrayUtil.toDoubleArray(pValues));
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
