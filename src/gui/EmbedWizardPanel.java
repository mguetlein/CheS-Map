package gui;

import gui.property.Property;
import gui.wizard.SimpleViewWizardPanel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import workflow.EmbedderProvider;
import alg.Algorithm;
import alg.embed3d.ThreeDEmbedder;
import alg.r.DistanceProperty;

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
