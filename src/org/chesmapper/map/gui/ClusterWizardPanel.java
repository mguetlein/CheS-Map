package org.chesmapper.map.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.alg.r.DistanceProperty;
import org.chesmapper.map.gui.wizard.SimpleViewWizardPanel;
import org.chesmapper.map.workflow.ClustererProvider;
import org.mg.javalib.gui.property.Property;

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
