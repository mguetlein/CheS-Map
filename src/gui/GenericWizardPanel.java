package gui;

import gui.property.PropertyPanel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;

import main.Settings;
import util.ImageLoader;
import alg.Algorithm;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.DefaultComponentFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import data.DatasetFile;

public abstract class GenericWizardPanel extends WizardPanel
{
	private JLabel infoIcon;
	JTextArea infoTextArea;
	IndexedRadioButton radioButtons[];
	ButtonGroup group;
	JPanel propertyPanel;
	//	PropertyPanel clusterPropertyPanel;

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

		propertyPanel = new JPanel(new CardLayout());
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

		//DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("fill:p:grow"));
		CellConstraints cc = new CellConstraints();
		int row = 1;
		setLayout(new FormLayout("fill:p:grow", "p,5dlu,p,15dlu,p,5dlu,p,10dlu,fill:p:grow"));

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
		add(p, cc.xy(1, row));
		row += 2;

		infoTextArea.setVisible(false);
		//builder.nextLine();

		DefaultFormBuilder rBuilder = new DefaultFormBuilder(new FormLayout("fill:p:grow"));
		for (JRadioButton b : radioButtons)
		{
			rBuilder.append(b);
			rBuilder.nextLine();
		}
		add(rBuilder.getPanel(), cc.xy(1, row));
		row += 2;

		//		builder.appendParagraphGapRow();
		//		builder.nextLine();

		//		builder.appendSeparator(getTitle() + " Properties");
		//		builder.nextLine();

		//		JPanel sep = new JPanel(new BorderLayout(5, 5));
		//		JLabel l = new JLabel(getTitle() + " Properties");
		//		l.setFont(l.getFont().deriveFont(Font.BOLD));
		//		sep.add(l, BorderLayout.WEST);
		//		JSeparator s = new JSeparator();
		//		sep.setAlignmentY(0.5f);
		//		sep.add(s);
		JComponent sep = DefaultComponentFactory.getInstance().createSeparator(getTitle() + " Properties");
		add(sep, cc.xy(1, row));
		row += 2;

		descriptionPanel = new DescriptionPanel(400);
		//		builder.append(descriptionPanel);
		add(descriptionPanel, cc.xy(1, row));
		row += 2;

		//		builder.appendParagraphGapRow();
		//		builder.nextLine();

		//		JPanel pp = new JPanel(new BorderLayout());
		//		pp.add(propertyDescriptionTextArea);
		//		pp.add(moreDescriptionButton, BorderLayout.EAST);
		//		builder.append(pp);

		//		JScrollPane scroll = new JScrollPane();
		//		scroll.add(propertyPanel);
		//		builder.append(scroll);

		//		builder.append(propertyPanel);
		add(propertyPanel, cc.xy(1, row));
		row += 2;
		//propertyPanel.setBorder(new EtchedBorder());

		//		setLayout(new BorderLayout());

		//		JScrollPane scroll = new JScrollPane(builder.getPanel());
		//		scroll.setBorder(null);
		//		add(scroll);

		//		add(builder.getPanel());

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

	HashMap<String, PropertyPanel> cards = new HashMap<String, PropertyPanel>();

	private void updateAlgorithmSelection(int index)
	{
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

		if (!cards.containsKey(selectedAlgorithm.toString()))
		{
			PropertyPanel clusterPropertyPanel = new PropertyPanel(selectedAlgorithm.getProperties(), Settings.PROPS,
					Settings.PROPERTIES_FILE);
			propertyPanel.add(clusterPropertyPanel, selectedAlgorithm.toString());
			cards.put(selectedAlgorithm.toString(), clusterPropertyPanel);
		}
		((CardLayout) propertyPanel.getLayout()).show(propertyPanel, selectedAlgorithm.toString());

		//		validate();
		//		repaint();
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
		cards.get(selectedAlgorithm.toString()).store();
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
		c.setProperties(cards.get(selectedAlgorithm.toString()).getProperties());
		return c;
	}

}
