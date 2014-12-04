package org.chesmapper.map.weka;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.Random;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;

import weka.clusterers.Clusterer;
import weka.clusterers.RandomizableClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Capabilities;
import weka.core.DenseInstance;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;

/**
 * cascade simple k means, selects the best k according to calinski-harabasz criterion
 * 
 * analogous to: http://cc.oulu.fi/~jarioksa/softhelp/vegan/html/cascadeKM.html
 * 
 * see Calinski, T. and J. Harabasz. 1974. A dendrite method for cluster analysis. Commun. Stat. 3: 1-27.
 * quoted in German: http://books.google.com/books?id=-f9Ox0p1-D4C&lpg=PA394&ots=SV3JfRIkQn&dq=Calinski%20and%20Harabasz&hl=de&pg=PA394#v=onepage&q&f=false
 * 
 * @author Martin Guetlein (martin.guetlein@gmail.com)
 */
public class CascadeSimpleKMeans extends RandomizableClusterer implements Clusterer
{
	protected int minNumClusters = 2;
	protected int maxNumClusters = 10;
	protected int restarts = 10;
	protected boolean printDebug = false;
	protected DistanceFunction distanceFunction = new EuclideanDistance();
	protected int maxIterations = 500;
	protected boolean manuallySelectNumClusters = false;

	protected SimpleKMeans kMeans = new SimpleKMeans();
	protected Instance meanInstance;
	protected int numInstances;
	protected DecimalFormat df = new DecimalFormat("#.##");

	@Override
	public void buildClusterer(Instances origData) throws Exception
	{
		ReplaceMissingValues rep = new ReplaceMissingValues();
		Instances data = new Instances(origData);
		rep.setInputFormat(data);
		data = Filter.useFilter(data, rep);

		meanInstance = new DenseInstance(data.numAttributes());
		for (int i = 0; i < data.numAttributes(); i++)
			meanInstance.setValue(i, data.meanOrMode(i));
		numInstances = data.numInstances();

		kMeans.setDistanceFunction(distanceFunction);
		kMeans.setMaxIterations(maxIterations);
		kMeans.setDontReplaceMissingValues(true);

		Random r = new Random(m_Seed);
		double meanCHs[] = new double[maxNumClusters + 1 - minNumClusters];
		double maxCHs[] = new double[maxNumClusters + 1 - minNumClusters];
		int maxSeed[] = new int[maxNumClusters + 1 - minNumClusters];

		TaskProvider.debug("CascadeKMeans Clustering, Restarts: " + restarts + ", K: " + maxNumClusters);

		for (int i = 0; i < restarts; i++)
		{
			if (printDebug)
				Settings.LOGGER.info("cascade> restarts: " + (i + 1) + " / " + restarts);

			for (int k = minNumClusters; k <= maxNumClusters; k++)
			{
				if (printDebug)
					Settings.LOGGER.info("cascade>  k:" + k + " ");

				TaskProvider.verbose("CascadeKMeans Clustering, Restarts: " + (i + 1) + "/" + restarts + ", K: " + k
						+ "/" + maxNumClusters);
				if (!TaskProvider.isRunning())
					return;

				int seed = r.nextInt();
				kMeans.setSeed(seed);
				kMeans.setNumClusters(k);
				kMeans.buildClusterer(data);
				double ch = getCalinskiHarabasz();

				int index = k - minNumClusters;
				meanCHs[index] = (meanCHs[index] * i + ch) / (double) (i + 1);
				if (i == 0 || ch > maxCHs[index])
				{
					maxCHs[index] = ch;
					maxSeed[index] = seed;
				}

				if (printDebug)
					Settings.LOGGER.info(" CH:" + df.format(ch) + "  W:"
							+ df.format(kMeans.getSquaredError() / (double) (numInstances - kMeans.getNumClusters()))
							+ " (unweighted:" + df.format(kMeans.getSquaredError()) + ")  B:"
							+ df.format(getSquaredErrorBetweenClusters() / (double) (kMeans.getNumClusters() - 1))
							+ " (unweighted:" + df.format(getSquaredErrorBetweenClusters()) + ") ");
			}
		}
		if (printDebug)
		{
			String s = "cascade> max CH: [ ";
			for (int i = 0; i < maxSeed.length; i++)
				s += df.format(maxCHs[i]) + " ";
			Settings.LOGGER.info(s + "]");
		}
		String s = "cascade> mean CH: [ ";
		for (int i = 0; i < maxSeed.length; i++)
			s += df.format(meanCHs[i]) + " ";
		Settings.LOGGER.info(s + "]");

		int bestK = -1;
		double maxCH = -1;
		for (int k = minNumClusters; k <= maxNumClusters; k++)
		{
			int index = k - minNumClusters;
			if (bestK == -1 || meanCHs[index] > maxCH)
			{
				maxCH = meanCHs[index];
				bestK = k;
			}
		}
		if (manuallySelectNumClusters)
		{
			int selectedK = selectKManually(meanCHs, bestK);
			if (selectedK != -1)
				bestK = selectedK;
		}
		int bestSeed = maxSeed[bestK - minNumClusters];

		Settings.LOGGER.info("cascade> k (yields highest mean CH): " + bestK);
		Settings.LOGGER.info("cascade> seed (highest CH for k=" + bestK + ") : " + bestSeed);

		kMeans.setSeed(bestSeed);
		kMeans.setNumClusters(bestK);
		kMeans.buildClusterer(data);
	}

	private int selectKManually(double[] meanCHs, int bestK)
	{
		DefaultTableModel m = new DefaultTableModel()
		{
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		JTable t = new JTable(m);
		t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
		{
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column)
			{
				return super.getTableCellRendererComponent(table, column == 1 ? df.format((Double) value) : value,
						isSelected, hasFocus, row, column);
			}
		});
		m.addColumn("Num clusters");
		m.addColumn("Mean CH score");
		for (int i = 0; i < meanCHs.length; i++)
			m.addRow(new Object[] { new Integer(minNumClusters + i), new Double(meanCHs[i]) });
		t.setRowSelectionInterval(bestK - minNumClusters, bestK - minNumClusters);
		JScrollPane s = new JScrollPane(t);
		if (meanCHs.length < 20)
			s.setPreferredSize(new Dimension(300, t.getRowHeight() * (meanCHs.length + 2)));
		JOptionPane.showConfirmDialog(null, s, "Select number of clusters", JOptionPane.DEFAULT_OPTION);
		return (t.getSelectedRow() + minNumClusters);
	}

	@Override
	public int clusterInstance(Instance instance) throws Exception
	{
		return kMeans.clusterInstance(instance);
	}

	private double getSquaredErrorBetweenClusters()
	{
		double errorSum = 0;
		for (int i = 0; i < kMeans.getNumClusters(); i++)
		{
			double dist = kMeans.getDistanceFunction().distance(kMeans.getClusterCentroids().instance(i), meanInstance);
			if (kMeans.getDistanceFunction() instanceof EuclideanDistance)//Euclidean distance to Squared Euclidean distance
				dist *= dist;
			dist *= kMeans.getClusterSizes()[i];
			errorSum += dist;
		}
		return errorSum;
	}

	/**
	 * see Calinski, T. and J. Harabasz. 1974. A dendrite method for cluster analysis. Commun. Stat. 3: 1-27.
	 * quoted in German: http://books.google.com/books?id=-f9Ox0p1-D4C&lpg=PA394&ots=SV3JfRIkQn&dq=Calinski%20and%20Harabasz&hl=de&pg=PA394#v=onepage&q&f=false
	 * 
	 * @param kMeans
	 * @param data
	 * @return
	 */
	private double getCalinskiHarabasz()
	{
		double betweenClusters = getSquaredErrorBetweenClusters() / (double) (kMeans.getNumClusters() - 1);
		double withinClusters = kMeans.getSquaredError() / (double) (numInstances - kMeans.getNumClusters());
		return betweenClusters / withinClusters;
	}

	@Override
	public double[] distributionForInstance(Instance instance) throws Exception
	{
		return kMeans.distributionForInstance(instance);
	}

	@Override
	public int numberOfClusters() throws Exception
	{
		return kMeans.numberOfClusters();
	}

	@Override
	public Capabilities getCapabilities()
	{
		return kMeans.getCapabilities();
	}

	public int getMinNumClusters()
	{
		return minNumClusters;
	}

	public void setMinNumClusters(int minNumClusters)
	{
		this.minNumClusters = minNumClusters;
	}

	public int getMaxNumClusters()
	{
		return maxNumClusters;
	}

	public void setMaxNumClusters(int maxNumClusters)
	{
		this.maxNumClusters = maxNumClusters;
	}

	public int getRestarts()
	{
		return restarts;
	}

	public void setRestarts(int restarts)
	{
		this.restarts = restarts;
	}

	public boolean isPrintDebug()
	{
		return printDebug;
	}

	public void setPrintDebug(boolean printDebug)
	{
		this.printDebug = printDebug;
	}

	public DistanceFunction getDistanceFunction()
	{
		return distanceFunction;
	}

	public void setDistanceFunction(DistanceFunction distanceFunction)
	{
		this.distanceFunction = distanceFunction;
	}

	public int getMaxIterations()
	{
		return maxIterations;
	}

	public void setMaxIterations(int maxIterations)
	{
		this.maxIterations = maxIterations;
	}

	public boolean isManuallySelectNumClusters()
	{
		return manuallySelectNumClusters;
	}

	public void setManuallySelectNumClusters(boolean manuallySelectNumClusters)
	{
		this.manuallySelectNumClusters = manuallySelectNumClusters;
	}

	public static void main(String args[]) throws Exception
	{
		Instances data = new Instances(new FileReader(new File("/home/martin/software/weka-3-6-6/data/cpu.arff")));
		CascadeSimpleKMeans c = new CascadeSimpleKMeans();
		c.setManuallySelectNumClusters(true);
		c.setPrintDebug(true);
		c.buildClusterer(data);
		System.exit(0);
	}
}
