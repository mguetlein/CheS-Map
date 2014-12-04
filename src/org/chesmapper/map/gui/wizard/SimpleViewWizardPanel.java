package org.chesmapper.map.gui.wizard;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.gui.CheSMapperWizard;
import org.chesmapper.map.main.PropHandler;
import org.chesmapper.map.workflow.SimpleViewAlgorithmProvider;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.gui.property.PropertyComponent;
import org.mg.javalib.gui.property.PropertyPanel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

public abstract class SimpleViewWizardPanel extends AbstractWizardPanel //implements AlgorithmMappingWorkflowProvider
{
	private JPanel buttons = new JPanel(new BorderLayout());
	private JButton button = new JButton();
	private boolean simpleSelected = false;
	private boolean yesSelected = true;
	private JPanel cardPanel = new JPanel(new CardLayout());
	private JPanel simpleContainer = new JPanel(new BorderLayout());
	private JPanel advancedContainer = new JPanel(new BorderLayout());
	private boolean selfUpdate = false;
	private static final String ADAVANCED = "Advanced >>";
	private static final String SIMPLE = "<< Simple";
	private PropertyPanel propPanel;

	protected SimpleViewAlgorithmProvider getAlgProvider()
	{
		return (SimpleViewAlgorithmProvider) algProvider;
	}

	public abstract String getSimpleQuestion();

	public SimpleViewWizardPanel(CheSMapperWizard wizard, SimpleViewAlgorithmProvider algProvider)
	{
		super(wizard, algProvider);
		this.wizard = wizard;

		buildLayout();
		addListeners();
		initListSelection();

		toggle(getAlgProvider().isSimpleSelectedFromProps());
	}

	private void addListeners()
	{
		addListListeners();

		ActionListener a = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				toggle(!simpleSelected);
			}
		};
		button.addActionListener(a);
	}

	private void buildLayout()
	{
		Dimension d = new JButton(ADAVANCED).getPreferredSize();
		button.setPreferredSize(d);
		setLayout(new BorderLayout(10, 10));
		buttons.add(button, BorderLayout.WEST);
		add(buttons, BorderLayout.NORTH);

		simpleContainer.add(createSimpleView());
		cardPanel.add(simpleContainer, "simple");

		advancedContainer.add(super.createListPanel());
		cardPanel.add(advancedContainer, "advanced");

		add(cardPanel);
	}

	protected JPanel createSimpleView()
	{
		yesSelected = getAlgProvider().isYesDefault();
		propPanel = null;
		if (getAlgProvider().getYesAlgorithm().getProperties() != null)
			propPanel = new PropertyPanel(getAlgProvider().getYesAlgorithm().getProperties(),
					PropHandler.getProperties(), PropHandler.getPropertiesFile());
		String yesText = "";
		if (yesSelected)
			yesText = "recommended";
		if (propPanel == null)
		{
			if (yesText.length() > 0)
				yesText += ", ";
			yesText += "applies '" + getAlgProvider().getYesAlgorithm().getName() + "'";
		}
		if (yesText.length() > 0)
			yesText = " (" + yesText + ")";
		final JRadioButton buttonYes = new JRadioButton("Yes" + yesText, yesSelected);
		JRadioButton buttonNo = new JRadioButton("No", !yesSelected);

		ButtonGroup group = new ButtonGroup();
		group.add(buttonYes);
		group.add(buttonNo);
		ActionListener a = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (buttonYes.isSelected() != yesSelected)
				{
					yesSelected = buttonYes.isSelected();
					if (propPanel != null)
						propPanel.setEnabled(yesSelected);
					wizard.update();
				}
			}
		};
		buttonYes.addActionListener(a);
		buttonNo.addActionListener(a);
		DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout(propPanel == null ? "p" : "p,20px,p"));
		b.setLineGapSize(Sizes.dluX(4));
		b.append(new JLabel(getSimpleQuestion()), b.getLayout().getColumnCount());
		b.nextLine();
		b.append(buttonYes);
		b.setLineGapSize(Sizes.dluX(4));
		if (propPanel != null)
		{
			JPanel props = new JPanel(new BorderLayout(5, 5));
			props.add(new JLabel("Applies '" + getAlgProvider().getYesAlgorithm().getName() + "'"), BorderLayout.NORTH);
			props.add(propPanel);
			b.append(props);
		}
		b.nextLine();
		b.append(buttonNo, b.getLayout().getColumnCount());
		b.setBorder(new EmptyBorder(5, 0, 0, 0));

		yesSelected = getAlgProvider().isYesSelectedFromProps();
		buttonYes.setSelected(yesSelected);
		buttonNo.setSelected(!yesSelected);
		if (propPanel != null)
			propPanel.setEnabled(yesSelected);

		return b.getPanel();
	}

	public PropertyComponent getComponentForProperty(Property p)
	{
		if (simpleSelected)
		{
			if (yesSelected)
			{
				if (propPanel == null)
					throw new IllegalArgumentException();
				for (int i = 0; i < getSelectedAlgorithm().getProperties().length; i++)
					if (getSelectedAlgorithm().getProperties()[i] == p)
						return propPanel.getComponentForProperty(p);
				throw new IllegalArgumentException();
			}
			else
				throw new IllegalArgumentException();
		}
		else
			return super.getComponentForProperty(p);
	}

	public void toggle(boolean simpleSelected)
	{
		if (selfUpdate)
			return;
		selfUpdate = true;
		this.simpleSelected = simpleSelected;
		button.setText(simpleSelected ? ADAVANCED : SIMPLE);
		((CardLayout) cardPanel.getLayout()).show(cardPanel, simpleSelected ? "simple" : "advanced");
		wizard.update();
		selfUpdate = false;
	}

	public Algorithm getSelectedAlgorithm()
	{
		if (simpleSelected)
			if (yesSelected)
				return getAlgProvider().getYesAlgorithm();
			else
				return getAlgProvider().getNoAlgorithm();
		else
			return listSelectedAlgorithm;
	}

	@Override
	public void proceed()
	{
		getAlgProvider().storeSimpleSelectionToProps(simpleSelected, yesSelected);
		super.proceed();
	}
}
