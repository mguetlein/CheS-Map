package org.chesmapper.map.gui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.alg.align3d.ManualAligner;
import org.chesmapper.map.alg.align3d.ThreeDAligner;
import org.chesmapper.map.gui.wizard.ListWizardPanel;
import org.chesmapper.map.workflow.AlignerProvider;
import org.mg.javalib.gui.property.Property;

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
