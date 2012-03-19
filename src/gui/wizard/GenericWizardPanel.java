package gui.wizard;

import gui.CheSMapperWizard;
import gui.FeatureWizardPanel.FeatureInfo;
import gui.Messages;
import gui.MoreTextPanel;
import gui.binloc.Binary;
import gui.property.PropertyPanel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import main.BinHandler;
import main.PropHandler;
import util.WizardComponentFactory;
import alg.Algorithm;
import alg.cluster.DatasetClusterer;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

import data.DatasetFile;

public abstract class GenericWizardPanel extends AdvancedSimpleWizardPanel
{
	DefaultListModel listModel;
	JList list;
	JScrollPane propertyScroll;
	JPanel propertyPanel;
	HashMap<String, PropertyPanel> cards = new HashMap<String, PropertyPanel>();

	protected CheSMapperWizard wizard;
	protected Algorithm selectedAlgorithm;
	private boolean binaryFound = true;

	protected static abstract class SimplePanel extends JPanel
	{
		protected abstract Algorithm getAlgorithm();

		protected abstract void store();
	}

	protected abstract boolean hasSimpleView();

	protected abstract SimplePanel createSimpleView();

	private SimplePanel simpleView;

	protected abstract Algorithm[] getAlgorithms();

	public GenericWizardPanel(CheSMapperWizard wizard)
	{
		this.wizard = wizard;
		buildLayout();
		addListeners();

		String method = PropHandler.get(getTitle() + "-method");
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
		if (hasSimpleView())
		{
			simpleView = createSimpleView();
			simple().add(simpleView);
			String simpleSelected = PropHandler.get(getTitle() + "-simple-selected");
			if (simpleSelected == null || simpleSelected.equals("true"))
				toggle(true);
		}
	}

	private void buildLayout()
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

		propertyPanel = new JPanel(new CardLayout());

		final JPanel p = new JPanel(new BorderLayout(10, 10));
		JPanel pp = new JPanel(new BorderLayout(5, 5));
		JLabel l = new JLabel(getTitle() + " Algorithms:");
		l.setFont(l.getFont().deriveFont(Font.BOLD));
		pp.add(l, BorderLayout.NORTH);
		pp.add(new JScrollPane(list));
		p.add(pp, BorderLayout.NORTH);

		JPanel pp2 = new JPanel(new BorderLayout(5, 5));
		JLabel l2 = new JLabel(getTitle() + " Properties:");
		l2.setFont(l2.getFont().deriveFont(Font.BOLD));
		pp2.add(l2, BorderLayout.NORTH);
		propertyScroll = WizardComponentFactory.getVerticalScrollPane(propertyPanel);

		pp2.add(propertyScroll);
		p.add(pp2);
		advanced().add(p);

	}

	private void addListeners()
	{
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
								updateAlgorithmSelection(list.getSelectedIndex(), true);
								GenericWizardPanel.this.wizard.update();
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
				updateAlgorithmSelection(list.getSelectedIndex(), false);
				GenericWizardPanel.this.wizard.update();
				lastSelected = list.getSelectedIndex();
			}
		});
	}

	private void updateAlgorithmSelection(int index, boolean forceUpdate)
	{
		if (selectedAlgorithm == getAlgorithms()[index] && !forceUpdate)
			return;

		selectedAlgorithm = getAlgorithms()[index];
		binaryFound = selectedAlgorithm.getBinary() == null || selectedAlgorithm.getBinary().isFound();

		if (!cards.containsKey(selectedAlgorithm.toString()))
		{
			DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("fill:p:grow"));
			builder.setLineGapSize(Sizes.dluX(16));

			MoreTextPanel descriptionPanel = new MoreTextPanel()
			{
				public Dimension getPreferredSize()
				{
					Dimension sup = super.getPreferredSize();
					if (sup.getHeight() > 180)
						sup.height = 180;
					return sup;
				}
			};
			descriptionPanel.addParagraph(selectedAlgorithm.getDescription());
			descriptionPanel.setDialogTitle(selectedAlgorithm.getName());
			descriptionPanel.setPreferredWith(GenericWizardPanel.this.getPreferredSize().width - 20);
			builder.append(descriptionPanel);

			if (selectedAlgorithm.getBinary() != null)
			{
				JComponent pp = BinHandler.getBinaryComponent(selectedAlgorithm.getBinary());
				pp.setBorder(new EmptyBorder(0, 0, 5, 0));
				builder.append(pp);
			}

			PropertyPanel clusterPropertyPanel = new PropertyPanel(selectedAlgorithm.getProperties(),
					PropHandler.getProperties(), PropHandler.getPropertiesFile());
			if (selectedAlgorithm.getProperties() != null)
			{
				if (selectedAlgorithm.getBinary() != null)
					builder.setLineGapSize(Sizes.dluX(8));
				builder.append(clusterPropertyPanel);
			}
			builder.getPanel().setName(selectedAlgorithm.toString());
			builder.setBorder(new EmptyBorder(2, 2, 2, 2));

			propertyPanel.add(builder.getPanel(), selectedAlgorithm.toString());
			// put panel in hash in has to store properties
			cards.put(selectedAlgorithm.toString(), clusterPropertyPanel);
		}
		((CardLayout) propertyPanel.getLayout()).show(propertyPanel, selectedAlgorithm.toString());

		// HACK: jsplitpane and cardlayout dont quite like themselves,
		// the preferred size of the card-layout-component has to be decreased by hand
		for (int i = 0; i < propertyPanel.getComponentCount(); i++)
		{
			if (((JPanel) propertyPanel.getComponent(i)).getName().equals(selectedAlgorithm.toString()))
			{
				propertyPanel.setPreferredSize(propertyPanel.getComponent(i).getPreferredSize());
				break;
			}
		}
		propertyScroll.getViewport().revalidate();
		propertyScroll.getViewport().setViewPosition(new Point(0, 0));
	}

	@Override
	public void proceed()
	{
		PropHandler.put(getTitle() + "-simple-selected", isSimpleSelected() ? "true" : "false");
		if (isSimpleSelected())
			simpleView.store();
		else
		{
			PropHandler.put(getTitle() + "-method", selectedAlgorithm.getName());
			cards.get(selectedAlgorithm.toString()).store();
		}
		PropHandler.storeProperties();

	}

	Messages msg;

	public void update(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		if (!isSimpleSelected() && !binaryFound)
			msg = Messages.errorMessage(""); // error is obvious (there is a error icon in the binary panel)
		else
			msg = getSelectedAlgorithm().getMessages(dataset, featureInfo, clusterer);
	}

	@Override
	public final Messages canProceed()
	{
		return msg;
	}

	protected int defaultSelection()
	{
		return -1;
	}

	public Algorithm getSelectedAlgorithm()
	{
		if (isSimpleSelected())
			return simpleView.getAlgorithm();
		else
			return selectedAlgorithm;
	}

	public void toggle(boolean simple)
	{
		super.toggle(simple);
		if (wizard != null)
			wizard.update();
	}

}
