package appdomain;

import java.util.List;

import alg.Algorithm;
import dataInterface.CompoundData;
import dataInterface.NominalProperty;
import dataInterface.NumericProperty;

public interface AppDomainComputer extends Algorithm
{
	public static final AppDomainComputer APP_DOMAIN_COMPUTERS[] = { KNNAppDomainComputer.INSTANCE,
			CentroidAppDomainComputer.INSTANCE, LeverageAppDomainComputer.INSTANCE,
			PropabilityDensityDomainComputer.INSTANCE };

	public void computeAppDomain(/*DatasetFile dataset,*/List<CompoundData> compounds,
			List<NumericProperty> features, double[][] featureDistanceMatrix);

	public NominalProperty getInsideAppDomainProperty();

	public NumericProperty getPropabilityAppDomainProperty();

}
