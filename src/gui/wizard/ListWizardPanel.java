package gui.wizard;

import gui.CheSMapperWizard;

import java.awt.BorderLayout;

import workflow.AlgorithmProvider;

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
