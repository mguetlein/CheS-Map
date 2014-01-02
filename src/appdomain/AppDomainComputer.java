package appdomain;

import java.util.List;

import alg.Algorithm;
import data.DatasetFile;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;

public interface AppDomainComputer extends Algorithm
{
	public static final AppDomainComputer APP_DOMAIN_COMPUTERS[] = { KNNAppDomainComputer.INSTANCE,
			CentroidAppDomainComputer.INSTANCE, LeverageAppDomainComputer.INSTANCE,
			PropabilityDensityDomainComputer.INSTANCE };

	public void computeAppDomain(DatasetFile dataset, List<CompoundData> compounds, List<CompoundProperty> features,
			double[][] featureDistanceMatrix);

	public CompoundProperty getInsideAppDomainProperty();

	public CompoundProperty getPropabilityAppDomainProperty();

}
