package org.chesmapper.map.gui.wizard;

import java.awt.BorderLayout;

import org.chesmapper.map.gui.CheSMapperWizard;
import org.chesmapper.map.workflow.AlgorithmProvider;

public abstract class ListWizardPanel extends AbstractWizardPanel
{
	public ListWizardPanel(CheSMapperWizard wizard, AlgorithmProvider algProvider)
	{
		super(wizard, algProvider);

		setLayout(new BorderLayout(10, 10));
		add(super.createListPanel());
		super.addListListeners();
		super.initListSelection();
	}
}
