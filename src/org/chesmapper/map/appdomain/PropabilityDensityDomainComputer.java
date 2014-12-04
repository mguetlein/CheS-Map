package org.chesmapper.map.appdomain;

import org.chesmapper.map.appdomain.stats.Tools;
import org.chesmapper.map.appdomain.stats.datastructures.Sort;
import org.chesmapper.map.appdomain.stats.transforms.densityestimation.KDensity1D;
import org.chesmapper.map.appdomain.stats.transforms.densityestimation.KNormal;
import org.chesmapper.map.appdomain.stats.transforms.densityestimation.Kernel;

import Jama.Matrix;

public class PropabilityDensityDomainComputer extends AbstractAppDomainComputer
{
	public static final AppDomainComputer INSTANCE = new PropabilityDensityDomainComputer();

	private PropabilityDensityDomainComputer()
	{
	}

	@Override
	public String getShortName()
	{
		return "prop";
	}

	private Kernel theKernel = new KNormal();
	private KDensity1D[] kdeList = null;
	private int nGrid = 1024;
	protected double threshold = 0;
	protected double pThreshold = 0.95;

	public boolean isEmpty()
	{
		return (kdeList == null);
	}

	public void clear()
	{
		clearKDEList();
	}

	protected void clearKDEList()
	{
		if (kdeList != null)
			for (int i = 0; i < kdeList.length; i++)
				kdeList[i] = null;
		kdeList = null;
	}

	protected void initKDE(int ndescriptors)
	{
		clearKDEList();
		kdeList = new KDensity1D[ndescriptors];
		for (int i = 0; i < kdeList.length; i++)
			kdeList[i] = null;
	}

	protected double[] mindata = null;
	protected double[] maxdata = null;

	@Override
	public void computeAppDomain()
	{
		//		if (featureDistanceMatrix == null)
		//			featureDistanceMatrix = EmbedUtil.euclMatrix(compounds, features, dataset);

		double d[][] = new double[compounds.size()][features.size()];
		for (int i = 0; i < d.length; i++)
		{
			for (int j = 0; j < d[0].length; j++)
			{
				d[i][j] = features.get(j).getNormalizedValues()[i];
			}
		}

		Matrix data = new Matrix(d);

		doEstimation(data, compounds.size(), features.size());

	}

	protected void doEstimation(Matrix matrix, int np, int nd)
	{
		double[][] points = matrix.getArray();
		//super.doEstimation(points,np,nd);
		mindata = new double[nd];
		maxdata = new double[nd];

		if (kdeList == null)
			initKDE(nd);
		double A[] = new double[np];

		KDensity1D akde;
		for (int d = 0; d < nd; d++)
		{
			for (int p = 0; p < np; p++)
				A[p] = points[p][d];
			akde = null;
			double datarange[] = new double[2];
			if (Tools.ExtendedInterval(A, np, datarange))
			{
				mindata[d] = datarange[0];
				maxdata[d] = datarange[1];
				akde = new KDensity1D();
				if (akde.estimateDensity(A, np, theKernel, nGrid, 0, datarange[0], datarange[1], null))
				{
				}
				else
					akde = null;
			}
			else
			{
				mindata[d] = Tools.min(A, np);
				maxdata[d] = Tools.max(A, np);
			}
			kdeList[d] = akde;
		}
		//TODO threshold
		//threshold
		A = doAssessment(matrix, np, nd);
		threshold = estimateThreshold(pThreshold, A);
		//A = null;

		pValues = A;

		for (int i = 0; i < A.length; i++)
		{
			//			pValues[i] = A[i];
			inside[i] = A[i] >= threshold;
		}
	}

	/**
	 *  does the actual assessment, to be overrided by inherited class
	 * @param points
	 * @param np
	 * @param nd
	 * @return double[]
	 */
	protected double[] doAssessment(Matrix points, int np, int nd)
	{
		double[] c = new double[np];
		for (int i = 0; i < np; i++)
			c[i] = doAssessment(points.getArray()[i], nd);
		return c;
	}

	protected double doAssessment(double[] points, int nd)
	{
		double result = 1;

		for (int d = 0; d < nd; d++)
		{
			//if outside range then set density to zero; 
			//TODO use extended range
			if ((points[d] < mindata[d]) || (points[d] > maxdata[d]))
			{
				result = 0;
				break;
			}
			else if (kdeList[d] != null)
				result = result * kdeList[d].getGridPoint(points[d]);
			else
			{
				result = 1; //if density not assessed reduces to ranges ...
			}
		}
		return result;
	}

	public int getDomain(double coverage)
	{
		int domain;
		if (coverage >= threshold)
			domain = 0;
		else
			domain = 1;
		return domain;
	}

	public int[] getDomain(double[] coverage)
	{
		if (coverage == null)
			return null;
		int np = coverage.length;
		int[] domain = new int[np];
		for (int i = 0; i < np; i++)
			//note the inequality , here it is >= in contrast to DataCoverageDistance			
			if (new Double(coverage[i]).isNaN())
				domain[i] = 2;
			else if (coverage[i] >= threshold)
				domain[i] = 0;
			else
				domain[i] = 1;
		return domain;
	}

	protected double estimateThreshold(double percent, double[] v)
	{
		double t;
		int tIndex = 0;
		//		if (percent == 1)
		//		{
		//			t = Tools.min(values, values.length);
		//		}
		//		else
		//		{
		double values[] = new double[v.length];
		for (int i = 0; i < values.length; i++)
			values[i] = v[i];
		Sort sort = new Sort();
		sort.QuickSortArray(values, values.length);
		sort = null;
		tIndex = (int) Math.round(values.length * (1 - percent));
		if (tIndex < 0)
			tIndex = 0;
		t = values[tIndex];
		//		}
		return t;

	}

}
