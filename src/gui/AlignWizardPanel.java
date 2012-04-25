package gui;

import gui.property.Property;
import gui.wizard.GenericWizardPanel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import main.Settings;
import alg.Algorithm;
import alg.align3d.ManualAligner;
import alg.align3d.ThreeDAligner;

public class AlignWizardPanel extends GenericWizardPanel
{
	boolean canProceed = false;

	public AlignWizardPanel(CheSMapperWizard w)
	{
		super(w);

		for (Algorithm a : getAlgorithms())
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

	@Override
	protected Algorithm[] getAlgorithms()
	{
		return ThreeDAligner.ALIGNER;
	}

	@Override
	public String getTitle()
	{
		return Settings.text("align.title");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("align.desc");
	}

	public ThreeDAligner getAlginer()
	{
		return (ThreeDAligner) getSelectedAlgorithm();
	}

	@Override
	protected boolean hasSimpleView()
	{
		return false;
	}

	@Override
	protected SimplePanel createSimpleView()
	{
		return null;
	}
}
