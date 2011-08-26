package gui;

import util.ArrayUtil;
import alg.cluster.DatasetClusterer;
import alg.cluster.NoClusterer;
import alg.cluster.StructuralClustererService;
import alg.cluster.WekaClusterer;
import data.DatasetFile;
import dataInterface.MoleculeProperty.Type;

public class ClusterWizardPanel extends GenericWizardPanel
{
	boolean canProceed = false;
	public static DatasetClusterer CLUSTERERS[];
	static
	{
		CLUSTERERS = ArrayUtil.concat(DatasetClusterer.class, new DatasetClusterer[] { new NoClusterer() },
				WekaClusterer.WEKA_CLUSTERER, new DatasetClusterer[] { new StructuralClustererService() });
	}

	public ClusterWizardPanel(CheSMapperWizard w)
	{
		super(w);
	}

	@Override
	public void update(DatasetFile dataset, int numFeatures, Type featureType)
	{
		if (!preconditionsMet)
			return;
		canProceed = !getDatasetClusterer().requiresNumericalFeatures() || numFeatures > 0;
		if (!canProceed)
			setInfo(getDatasetClusterer().getName()
					+ " requires numerical features, you have no features selected.\nPlease select numerical features in previous step, or select another cluster method.",
					MsgType.ERROR);
		else if (getDatasetClusterer().getFixedNumClustersProperty() != null)
			setInfo("This clusterer returns a fixed number of clusters (set property '"
					+ getDatasetClusterer().getFixedNumClustersProperty() + "').", MsgType.INFO);
		else
			setInfo("", MsgType.EMPTY);
	}

	@Override
	public boolean canProceed()
	{
		if (!preconditionsMet)
			return false;
		return canProceed;
	}

	@Override
	public String getTitle()
	{
		return "Cluster dataset";
	}

	@Override
	public String getDescription()
	{
		return "Divides a dataset into clusters (i.e. into subsets of compounds)";
	}

	@Override
	protected DatasetClusterer[] getAlgorithms()
	{
		return CLUSTERERS;
	}

	public DatasetClusterer getDatasetClusterer()
	{
		return (DatasetClusterer) getSelectedAlgorithm();
	}

}
