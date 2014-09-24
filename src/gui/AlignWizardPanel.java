package gui;

import gui.property.Property;
import gui.wizard.ListWizardPanel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import workflow.AlignerProvider;
import alg.Algorithm;
import alg.align3d.ManualAligner;
import alg.align3d.ThreeDAligner;

public class AlignWizardPanel extends ListWizardPanel
{
	public AlignWizardPanel(CheSMapperWizard w)
	{
		super(w, new AlignerProvider());

		for (Algorithm a : algProvider.getAlgorithms())
		{
			if (a instanceof ManualAligner)
			{
				for (Property p : a.getProperties())
				{
					p.addPropertyChangeListener(new PropertyChangeListener()
					{
						@Override
						public void propertyChange(PropertyChangeEvent evt)
						{
							wizard.update();
						}
					});
				}
			}
		}
	}

	public ThreeDAligner getAlginer()
	{
		return (ThreeDAligner) getSelectedAlgorithm();
	}
}
