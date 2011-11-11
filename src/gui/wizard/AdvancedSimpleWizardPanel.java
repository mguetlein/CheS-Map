package gui.wizard;

import gui.WizardPanel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;

import javax.swing.JButton;
import javax.swing.JPanel;

public abstract class AdvancedSimpleWizardPanel extends WizardPanel
{
	JPanel buttons = new JPanel(new BorderLayout());
	private JButton button = new JButton();
	private boolean simple = false;
	JPanel cardPanel = new JPanel(new CardLayout());
	private JPanel simpleContainer = new JPanel(new BorderLayout());
	private JPanel advancedContainer = new JPanel(new BorderLayout());
	private boolean selfUpdate = false;

	public static final String ADAVANCED = "Advanced >>";
	public static final String SIMPLE = "<< Simple";

	public AdvancedSimpleWizardPanel()
	{
		Dimension d = new JButton(ADAVANCED).getPreferredSize();
		button.setPreferredSize(d);
		setLayout(new BorderLayout(10, 10));
		buttons.add(button, BorderLayout.WEST);

		ActionListener a = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				toggle(!simple);
			}
		};
		button.addActionListener(a);
		buttons.setVisible(false);
		add(buttons, BorderLayout.NORTH);

		cardPanel.add(simpleContainer, "simple");
		cardPanel.add(advancedContainer, "advanced");

		add(cardPanel);

		this.addContainerListener(new ContainerAdapter()
		{
			@Override
			public void componentAdded(ContainerEvent e)
			{
				throw new IllegalArgumentException("use wizardpanel.simple().add or wizardpanel.advanced().add instead");
			}
		});
		toggle(false);
	}

	protected JPanel simple()
	{
		buttons.setVisible(true);
		return simpleContainer;
	}

	protected JPanel advanced()
	{
		return advancedContainer;
	}

	protected boolean isSimpleSelected()
	{
		return simple;
	}

	public void toggle(boolean simple)
	{
		if (selfUpdate)
			return;
		selfUpdate = true;
		this.simple = simple;
		button.setText(simple ? ADAVANCED : SIMPLE);
		((CardLayout) cardPanel.getLayout()).show(cardPanel, simple ? "simple" : "advanced");
		selfUpdate = false;
	}
}
