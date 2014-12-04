package org.chesmapper.map.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.alg.embed3d.ThreeDEmbedder;
import org.chesmapper.map.alg.r.DistanceProperty;
import org.chesmapper.map.gui.wizard.SimpleViewWizardPanel;
import org.chesmapper.map.workflow.EmbedderProvider;
import org.mg.javalib.gui.property.Property;

public class EmbedWizardPanel extends SimpleViewWizardPanel
{
	public EmbedWizardPanel(CheSMapperWizard w)
	{
		super(w, new EmbedderProvider());

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

	public ThreeDEmbedder get3DEmbedder()
	{
		return (ThreeDEmbedder) getSelectedAlgorithm();
	}

	@Override
	public String getSimpleQuestion()
	{
		return "Embedd compounds according to their feature values into 3D space?";
	}

}
