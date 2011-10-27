package gui;

import main.Settings;
import util.ArrayUtil;
import alg.cluster.DatasetClusterer;
import alg.cluster.NoClusterer;
import alg.cluster.WekaClusterer;
import alg.cluster.r.AbstractRClusterer;

public class ClusterWizardPanel extends GenericWizardPanel
{
	boolean canProceed = false;
	public static DatasetClusterer CLUSTERERS[];
	static
	{
		CLUSTERERS = ArrayUtil.concat(DatasetClusterer.class, new DatasetClusterer[] { new NoClusterer() },
				AbstractRClusterer.R_CLUSTERER, WekaClusterer.WEKA_CLUSTERER
		//,new DatasetClusterer[] { new StructuralClustererService() }
				);
	}

	public ClusterWizardPanel(CheSMapperWizard w)
	{
		super(w);
	}

	@Override
	public String getTitle()
	{
		return Settings.text("cluster.title");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("cluster.desc");
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
