package workflow;

import gui.property.IntegerProperty;
import gui.property.Property;
import main.Settings;
import weka.CascadeSimpleKMeans;
import alg.cluster.DatasetClusterer;
import alg.cluster.NoClusterer;
import alg.cluster.WekaClusterer;

public class ClustererProvider extends AbstractSimpleViewAlgorithmProvider
{
	private static IntegerProperty min = new IntegerProperty("minNumClusters", 2, 1, Integer.MAX_VALUE);
	private static IntegerProperty max = new IntegerProperty("maxNumClusters", 5, 1, Integer.MAX_VALUE);
	private static Property[] DEFAULT_CLUSTERER_PROPS = new Property[] { min, max };
	static
	{
		min.setDisplayName("minimum number of clusters");
		max.setDisplayName("maximum number of clusters");
	}
	private static final DatasetClusterer DEFAULT = WekaClusterer.getNewInstance(new CascadeSimpleKMeans(),
			DEFAULT_CLUSTERER_PROPS);

	@Override
	public DatasetClusterer[] getAlgorithms()
	{
		return DatasetClusterer.CLUSTERERS;
	}

	@Override
	public int getDefaultListSelection()
	{
		return 2;
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

	public boolean isYesDefault()
	{
		return false;
	}

	@Override
	public DatasetClusterer getYesAlgorithm()
	{
		return DEFAULT;
	}

	@Override
	public DatasetClusterer getNoAlgorithm()
	{
		return NoClusterer.INSTANCE;
	}

}
