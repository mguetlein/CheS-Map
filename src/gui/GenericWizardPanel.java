package gui;

import gui.binloc.Binary;
import gui.property.PropertyPanel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import main.Settings;
import util.ImageLoader;
import alg.Algorithm;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.DefaultComponentFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;

import data.DatasetFile;
import dataInterface.MoleculeProperty.Type;

public abstract class GenericWizardPanel extends WizardPanel
{
	private JLabel infoIcon;
	JTextArea infoTextArea;

	DefaultListModel listModel;
	JList list;

	JPanel propertyPanel;

	protected Algorithm selectedAlgorithm;
	protected boolean preconditionsMet = true;

	protected abstract String getAlgorithmType();

	protected abstract Algorithm[] getAlgorithms();

	protected enum MsgType
	{
		INFO, WARNING, ERROR, EMPTY
	}

	public GenericWizardPanel(final CheSMapperWizard w)
	{
		listModel = new DefaultListModel();
		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setVisibleRowCount(7);
		final Font defaultFont = ((JLabel) list.getCellRenderer()).getFont();
		final Font disabledFont = defaultFont.deriveFont(Font.ITALIC);
		final Color defaultColor = ((JLabel) list.getCellRenderer()).getForeground();
		final Color disabledColor = defaultColor.brighter().brighter();
		list.setCellRenderer(new DefaultListCellRenderer()
		{

			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				Algorithm a = (Algorithm) value;
				JLabel l = (JLabel) super.getListCellRendererComponent(list, a.getName(), index, isSelected,
						cellHasFocus);
				if (a.getBinary() != null && !a.getBinary().isFound())
				{
					l.setFont(disabledFont);
					l.setForeground(disabledColor);
				}
				else
				{
					l.setFont(defaultFont);
					l.setForeground(defaultColor);
				}
				return l;
			}
		});
		for (final Algorithm algorithm : getAlgorithms())
		{
			listModel.addElement(algorithm);
			final Binary bin = algorithm.getBinary();
			if (bin != null)
				bin.addPropertyChangeListener(new PropertyChangeListener()
				{
					@Override
					public void propertyChange(PropertyChangeEvent evt)
					{
						if (bin.isFound())
						{
							list.repaint();
							if (list.getSelectedValue() == algorithm)
							{
								updateAlgorithmSelection(list.getSelectedIndex());
								w.update();
							}
						}
					}
				});
		}
		list.addListSelectionListener(new ListSelectionListener()
		{
			int lastSelected = 0;

			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (list.getSelectedIndex() == -1)
				{
					list.setSelectedValue(getAlgorithms()[lastSelected], true);
					return;
				}
				//				Algorithm a = (Algorithm) list.getSelectedValue();
				updateAlgorithmSelection(list.getSelectedIndex());
				w.update();

				lastSelected = list.getSelectedIndex();
			}
		});

		propertyPanel = new JPanel(new CardLayout());

		CellConstraints cc = new CellConstraints();
		int row = 1;
		setLayout(new FormLayout("fill:p:grow", "p,3dlu,p,5dlu,p,15dlu,p,3dlu,fill:p:grow"));

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

		setInfo("", MsgType.EMPTY);

		add(new JLabel(getAlgorithmType() + ":"), cc.xy(1, row));
		row += 2;

		add(new JScrollPane(list), cc.xy(1, row));
		row += 2;

		add(p, cc.xy(1, row));
		row += 2;

		JComponent sep = DefaultComponentFactory.getInstance().createSeparator(getTitle() + " Properties");
		add(sep, cc.xy(1, row));
		row += 2;

		add(propertyPanel, cc.xy(1, row));
		row += 2;

		String method = (String) Settings.PROPS.get(getTitle() + "-method");
		boolean selected = false;
		if (method != null)
		{
			for (Algorithm a : getAlgorithms())
			{
				if (a.getName().equals(method))
				{
					list.setSelectedValue(a, true);
					selected = true;
					break;
				}
			}
		}
		if (!selected)
		{
			int selection = defaultSelection();
			if (selection == -1)
			{
				selection = 0;
				for (int i = 0; i < getAlgorithms().length; i++)
					if (getAlgorithms()[i].getBinary() == null || getAlgorithms()[i].getBinary().isFound())
					{
						selection = i;
						break;
					}
			}
			list.setSelectedValue(getAlgorithms()[selection], true);
		}
	}

	HashMap<String, PropertyPanel> cards = new HashMap<String, PropertyPanel>();

	private void updateAlgorithmSelection(int index)
	{
		selectedAlgorithm = getAlgorithms()[index];

		preconditionsMet = selectedAlgorithm.getBinary() == null || selectedAlgorithm.getBinary().isFound();
		setInfo("", MsgType.EMPTY);

		if (!cards.containsKey(selectedAlgorithm.toString()))
		{
			DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("fill:p:grow"));
			builder.setLineGapSize(Sizes.dluX(16));

			MoreTextPanel descriptionPanel = new MoreTextPanel();
			descriptionPanel.addParagraph(selectedAlgorithm.getDescription());
			descriptionPanel.setDialogTitle(selectedAlgorithm.getName());
			descriptionPanel.setPreferredWith(GenericWizardPanel.this.getPreferredSize().width - 20);
			builder.append(descriptionPanel);

			// layout HACK
			// top:100:grow has the side effect of add large gaps between following rows
			// this ugly when the description panel is very small
			// -> use 'top:100:grow' only for large descriptions, else use 'p'
			if (descriptionPanel.getPreferredSize().getHeight() > 180)
				builder.getLayout().setRowSpec(builder.getRow(),
						new RowSpec(RowSpec.TOP, Sizes.pixel(100), RowSpec.DEFAULT_GROW));
			else
				builder.getLayout().setRowSpec(builder.getRow(),
						new RowSpec(RowSpec.TOP, Sizes.PREFERRED, RowSpec.NO_GROW));

			if (selectedAlgorithm.getBinary() != null)
			{
				JComponent pp = Settings.getBinaryComponent(selectedAlgorithm.getBinary());
				pp.setBorder(new EmptyBorder(0, 0, 5, 0));

				builder.append(pp);
				builder.getLayout().setRowSpec(builder.getRow(),
						new RowSpec(RowSpec.TOP, Sizes.PREFERRED, RowSpec.NO_GROW));
			}

			PropertyPanel clusterPropertyPanel = new PropertyPanel(selectedAlgorithm.getProperties(), Settings.PROPS,
					Settings.PROPERTIES_FILE);
			if (selectedAlgorithm.getProperties() != null)
			{
				if (selectedAlgorithm.getBinary() != null)
					builder.setLineGapSize(Sizes.dluX(8));

				builder.append(clusterPropertyPanel);
				builder.getLayout().setRowSpec(builder.getRow(),
						new RowSpec(RowSpec.TOP, Sizes.PREFERRED, RowSpec.NO_GROW));
			}
			propertyPanel.add(builder.getPanel(), selectedAlgorithm.toString());
			cards.put(selectedAlgorithm.toString(), clusterPropertyPanel);
		}
		((CardLayout) propertyPanel.getLayout()).show(propertyPanel, selectedAlgorithm.toString());
	}

	public void update(DatasetFile dataset, int numFeatures, Type featureType)
	{
		if (canProceed() && getSelectedAlgorithm().getWarning() != null)
			setInfo(getSelectedAlgorithm().getWarning(), MsgType.WARNING);
	}

	protected int defaultSelection()
	{
		return -1;
	}

	protected void setInfo(String string, MsgType type)
	{
		//infoTextArea.setVisible(type != MsgType.EMPTY);
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
				infoIcon.setIcon(new ImageIcon()
				{
					public int getIconHeight()
					{
						return 16;
					}

					public int getIconWidth()
					{
						return 16;
					}
				});
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
		return selectedAlgorithm;
	}

}
