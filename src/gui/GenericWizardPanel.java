package gui;

import gui.property.PropertyPanel;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import main.Settings;
import util.ImageLoader;
import alg.Algorithm;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import data.DatasetFile;

public abstract class GenericWizardPanel extends WizardPanel
{
	private JLabel infoIcon;
	JTextArea infoTextArea;
	IndexedRadioButton radioButtons[];
	ButtonGroup group;
	JPanel propertyPanel;
	PropertyPanel clusterPropertyPanel;

	DescriptionPanel descriptionPanel;

	protected Algorithm selectedAlgorithm;
	protected boolean preconditionsMet = true;

	protected abstract Algorithm[] getAlgorithms();

	protected enum MsgType
	{
		INFO, WARNING, ERROR, EMPTY
	}

	class IndexedRadioButton extends JRadioButton
	{
		int index;

		public IndexedRadioButton(String text, int index)
		{
			super(text);
			this.index = index;
		}
	}

	public GenericWizardPanel(final CheSMapperWizard w)
	{
		group = new ButtonGroup();

		propertyPanel = new JPanel(new BorderLayout());
		radioButtons = new IndexedRadioButton[getAlgorithms().length];

		int bCount = 0;
		for (Algorithm algorithm : getAlgorithms())
		{
			IndexedRadioButton b = new IndexedRadioButton(algorithm.getName(), bCount);
			if (algorithm.getPreconditionErrors() != null)
			{
				b.setFont(b.getFont().deriveFont(Font.ITALIC));
				b.setForeground(b.getForeground().brighter().brighter());
			}
			b.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					updateAlgorithmSelection(((IndexedRadioButton) e.getSource()).index);
					w.update();
				}
			});
			group.add(b);
			radioButtons[bCount++] = b;
		}

		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("fill:p:grow"));

		infoIcon = new JLabel();
		infoTextArea = new JTextArea();
		infoTextArea.setFont(infoTextArea.getFont().deriveFont(Font.BOLD));
		infoTextArea.setBorder(null);
		infoTextArea.setEditable(false);
		infoTextArea.setOpaque(false);
		infoTextArea.setWrapStyleWord(true);
		infoTextArea.setLineWrap(true);
		JPanel p = new JPanel(new BorderLayout(5, 0));
		p.add(infoIcon, BorderLayout.WEST);
		p.add(infoTextArea);
		builder.append(p);
		infoTextArea.setVisible(false);
		builder.nextLine();

		for (JRadioButton b : radioButtons)
		{
			builder.append(b);
			builder.nextLine();
		}

		builder.appendParagraphGapRow();
		builder.nextLine();

		builder.appendSeparator(getTitle() + " Properties");
		builder.nextLine();

		descriptionPanel = new DescriptionPanel();
		builder.append(descriptionPanel);

		builder.appendParagraphGapRow();
		builder.nextLine();

		//		JPanel pp = new JPanel(new BorderLayout());
		//		pp.add(propertyDescriptionTextArea);
		//		pp.add(moreDescriptionButton, BorderLayout.EAST);
		//		builder.append(pp);

		//		JScrollPane scroll = new JScrollPane();
		//		scroll.add(propertyPanel);
		//		builder.append(scroll);
		builder.append(propertyPanel);

		setLayout(new BorderLayout());
		JScrollPane scroll = new JScrollPane(builder.getPanel());
		scroll.setBorder(null);
		add(scroll);
		//add(builder.getPanel());

		String method = (String) Settings.PROPS.get(getTitle() + "-method");
		boolean selected = false;
		if (method != null)
		{
			for (IndexedRadioButton b : radioButtons)
			{
				if (b.getText().equals(method))
				{
					b.setSelected(true);
					selected = true;
					updateAlgorithmSelection(b.index);
					break;
				}
			}
		}
		if (!selected)
		{
			int firstWorking = 0;
			for (int i = 0; i < getAlgorithms().length; i++)
				if (getAlgorithms()[i].getPreconditionErrors() == null)
				{
					firstWorking = i;
					break;
				}
			radioButtons[firstWorking].setSelected(true);
			updateAlgorithmSelection(firstWorking);
		}
	}

	private void updateAlgorithmSelection(int index)
	{
		setIgnoreRepaint(true);
		propertyPanel.removeAll();
		selectedAlgorithm = getAlgorithms()[index];

		descriptionPanel.setText(selectedAlgorithm.getName(), selectedAlgorithm.getDescription());

		preconditionsMet = true;
		String errors = selectedAlgorithm.getPreconditionErrors();
		if (errors != null)
		{
			setInfo(errors, MsgType.ERROR);
			preconditionsMet = false;
		}
		else
			setInfo("", MsgType.EMPTY);

		clusterPropertyPanel = new PropertyPanel(selectedAlgorithm.getProperties(), Settings.PROPS,
				Settings.PROPERTIES_FILE);
		propertyPanel.add(clusterPropertyPanel);
		setIgnoreRepaint(false);
		validate();
		repaint();
	}

	public void update(DatasetFile dataset, int numNumericFeatures)
	{
	}

	protected void setInfo(String string, MsgType type)
	{
		infoTextArea.setVisible(type != MsgType.EMPTY);
		infoTextArea.setText(string);
		switch (type)
		{
			case INFO:
				infoIcon.setIcon(ImageLoader.INFO);
				break;
			case WARNING:
				infoIcon.setIcon(ImageLoader.WARNING);
				break;
			case ERROR:
				infoIcon.setIcon(ImageLoader.ERROR);
				break;
			case EMPTY:
				infoIcon.setIcon(null);
				break;
		}
	}

	@Override
	public void proceed()
	{
		Settings.PROPS.put(getTitle() + "-method", selectedAlgorithm.getName());
		Settings.storeProps();
		clusterPropertyPanel.store();
	}

	@Override
	public boolean canProceed()
	{
		return preconditionsMet;
	}

	public Algorithm getSelectedAlgorithm()
	{
		//		Algorithm c = null;
		//		try
		//		{
		//			c = (Algorithm) Class.forName(selectedAlgorithm.getClass().getName()).newInstance();
		//		}
		//		catch (Exception e)
		//		{
		//			e.printStackTrace();
		//		}

		Algorithm c = (Algorithm) selectedAlgorithm;
		c.setProperties(clusterPropertyPanel.getProperties());
		return c;
	}

}
