package gui;

import gui.property.Property;
import gui.wizard.SimpleViewWizardPanel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import workflow.ClustererProvider;
import alg.Algorithm;
import alg.cluster.DatasetClusterer;
import alg.r.DistanceProperty;

public class ClusterWizardPanel extends SimpleViewWizardPanel
{
	public ClusterWizardPanel(CheSMapperWizard w)
	{
		super(w, new ClustererProvider());

		for (Algorithm a : algProvider.getAlgorithms())
			if (a.getProperties() != null)
				for (Property p : a.getProperties())
					if (p instanceof DistanceProperty)
						p.addPropertyChangeListener(new PropertyChangeListener()
						{
							@Override
							public void propertyChange(PropertyChangeEvent evt)
							{
								wizard.update();
							}
						});
	}

	public DatasetClusterer getDatasetClusterer()
	{
		return (DatasetClusterer) getSelectedAlgorithm();
	}

	@Override
	public String getSimpleQuestion()
	{
		return "Cluster dataset?";
	}
}
