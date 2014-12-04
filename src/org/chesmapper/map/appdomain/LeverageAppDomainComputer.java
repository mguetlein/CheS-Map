package org.chesmapper.map.appdomain;

import Jama.Matrix;

public class LeverageAppDomainComputer extends AbstractAppDomainComputer
{
	public static final AppDomainComputer INSTANCE = new LeverageAppDomainComputer();

	private LeverageAppDomainComputer()
	{
	}

	@Override
	public String getShortName()
	{
		return "leverage";
	}

	Matrix XTX1;
	Matrix hat;

	@Override
	public void computeAppDomain()
	{
		double d[][] = new double[compounds.size()][features.size()];
		for (int i = 0; i < d.length; i++)
		{
			for (int j = 0; j < d[0].length; j++)
			{
				d[i][j] = features.get(j).getNormalizedValues()[i];
			}
		}
		Matrix data = new Matrix(d);

		double threshold = 3.0 * (data.getColumnDimension() + 1) / (data.getRowDimension());

		Matrix X = data;

		Matrix XT = X.transpose();

		Matrix tmp = XT.times(X);
		//(Xtransposed * X)^-1 , will be needed for estimation
		XTX1 = tmp.inverse();
		tmp = null;
		tmp = X.times(XTX1);
		hat = tmp.times(XT);
		tmp = null;

		Matrix x = data;
		Matrix xt = x.transpose();
		Matrix h = x.times(XTX1).times(xt);
		double[] leverage = new double[data.getRowDimension()];
		for (int i = 0; i < leverage.length; i++)
			leverage[i] = h.get(i, i);

		for (int i = 0; i < leverage.length; i++)
		{
			pValues[i] = leverage[i];
			inside[i] = leverage[i] <= threshold;
		}
	}

}
