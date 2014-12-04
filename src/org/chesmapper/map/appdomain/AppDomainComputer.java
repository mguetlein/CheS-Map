package org.chesmapper.map.appdomain;

import java.util.List;

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;

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
