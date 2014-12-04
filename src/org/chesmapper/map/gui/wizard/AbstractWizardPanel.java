package org.chesmapper.map.gui.wizard;

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

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.gui.CheSMapperWizard;
import org.chesmapper.map.gui.FeatureWizardPanel.FeatureInfo;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.PropHandler;
import org.chesmapper.map.util.WizardComponentFactory;
import org.chesmapper.map.workflow.AlgorithmProvider;
import org.mg.javalib.gui.Messages;
import org.mg.javalib.gui.MoreTextPanel;
import org.mg.javalib.gui.WizardPanel;
import org.mg.javalib.gui.binloc.Binary;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.gui.property.PropertyComponent;
import org.mg.javalib.gui.property.PropertyPanel;
import org.mg.javalib.util.ArrayUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

public abstract class AbstractWizardPanel extends WizardPanel
{
	DefaultListModel<Algorithm> listModel;
	JList<Algorithm> list;
	JScrollPane propertyScroll;
	JPanel propertyPanel;
	HashMap<String, PropertyPanel> cards = new HashMap<String, PropertyPanel>();

	protected CheSMapperWizard wizard;
	protected Algorithm listSelectedAlgorithm;
	private boolean binaryFound = true;

	protected AlgorithmProvider algProvider;

	public AbstractWizardPanel(CheSMapperWizard wizard, AlgorithmProvider algProvider)
	{
		this.wizard = wizard;
		this.algProvider = algProvider;
		if (wizard == null)
			throw new IllegalArgumentException("not longer supported");
	}

	@Override
	public final String getTitle()
	{
		return algProvider.getTitle();
	}

	@Override
	public final String getDescription()
	{
		return algProvider.getDescription();
	}

	protected JPanel createListPanel()
	{
		listModel = new DefaultListModel<Algorithm>();
		list = new JList<Algorithm>(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setVisibleRowCount(7);
		final Font defaultFont = ((JLabel) list.getCellRenderer()).getFont();
		final Font disabledFont = defaultFont.deriveFont(Font.ITALIC);
		final Color defaultColor = ((JLabel) list.getCellRenderer()).getForeground();
		final Color disabledColor = defaultColor.brighter().brighter();
		list.setCellRenderer(new DefaultListCellRenderer()
		{

			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
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

		return p;
	}

	protected void initListSelection()
	{
		Algorithm alg = algProvider.getListAlgorithmFromProps();
		int idx = ArrayUtil.indexOf(algProvider.getAlgorithms(), alg);
		if (idx == -1)
			idx = algProvider.getDefaultListSelection();
		if (idx == -1)
		{
			for (int i = 0; i < algProvider.getAlgorithms().length; i++)
				if (algProvider.getAlgorithms()[i].getBinary() == null
						|| algProvider.getAlgorithms()[i].getBinary().isFound())
				{
					idx = i;
					break;
				}
		}
		if (idx == -1)
			throw new IllegalStateException();
		list.setSelectedIndex(idx);
	}

	protected void addListListeners()
	{
		for (final Algorithm algorithm : algProvider.getAlgorithms())
		{
			listModel.addElement(algorithm);
			final Binary bin = algorithm.getBinary();
			if (bin != null)
				bin.addPropertyChangeListener(new PropertyChangeListener()
				{
					@Override
					public void propertyChange(PropertyChangeEvent evt)
					{
						if (wizard.isClosed())
							return;
						if (bin.isFound())
						{
							list.repaint();
							if (list.getSelectedValue() == algorithm)
							{
								updateAlgorithmSelection(list.getSelectedIndex(), true);
								AbstractWizardPanel.this.wizard.update();
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
					list.setSelectedValue(algProvider.getAlgorithms()[lastSelected], true);
					return;
				}
				updateAlgorithmSelection(list.getSelectedIndex(), false);
				AbstractWizardPanel.this.wizard.update();
				lastSelected = list.getSelectedIndex();
			}
		});
	}

	private void updateAlgorithmSelection(int index, boolean forceUpdate)
	{
		if (listSelectedAlgorithm == algProvider.getAlgorithms()[index] && !forceUpdate)
			return;

		listSelectedAlgorithm = algProvider.getAlgorithms()[index];
		binaryFound = listSelectedAlgorithm.getBinary() == null || listSelectedAlgorithm.getBinary().isFound();

		if (!cards.containsKey(listSelectedAlgorithm.toString()))
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
			descriptionPanel.addParagraph(listSelectedAlgorithm.getDescription());
			descriptionPanel.setDialogTitle(listSelectedAlgorithm.getName());
			descriptionPanel.setPreferredWith(AbstractWizardPanel.this.getPreferredSize().width - 20);
			builder.append(descriptionPanel);

			if (listSelectedAlgorithm.getBinary() != null)
			{
				JComponent pp = BinHandler.getBinaryComponent(listSelectedAlgorithm.getBinary(), wizard);
				pp.setBorder(new EmptyBorder(0, 0, 5, 0));
				builder.append(pp);
			}

			PropertyPanel clusterPropertyPanel = new PropertyPanel(listSelectedAlgorithm.getProperties(),
					PropHandler.getProperties(), PropHandler.getPropertiesFile());

			if (listSelectedAlgorithm.getProperties() != null)
			{
				if (listSelectedAlgorithm.getBinary() != null)
					builder.setLineGapSize(Sizes.dluX(8));
				builder.append(clusterPropertyPanel);
			}
			builder.getPanel().setName(listSelectedAlgorithm.toString());
			builder.setBorder(new EmptyBorder(2, 2, 2, 2));

			propertyPanel.add(builder.getPanel(), listSelectedAlgorithm.toString());
			// put panel in hash in has to store properties
			cards.put(listSelectedAlgorithm.toString(), clusterPropertyPanel);
		}
		((CardLayout) propertyPanel.getLayout()).show(propertyPanel, listSelectedAlgorithm.toString());

		// HACK: jsplitpane and cardlayout dont quite like themselves,
		// the preferred size of the card-layout-component has to be decreased by hand
		for (int i = 0; i < propertyPanel.getComponentCount(); i++)
		{
			if (((JPanel) propertyPanel.getComponent(i)).getName().equals(listSelectedAlgorithm.toString()))
			{
				propertyPanel.setPreferredSize(propertyPanel.getComponent(i).getPreferredSize());
				break;
			}
		}
		propertyScroll.getViewport().revalidate();
		propertyScroll.getViewport().setViewPosition(new Point(0, 0));
	}

	public PropertyComponent getComponentForProperty(Property p)
	{
		if (getSelectedAlgorithm() == null || getSelectedAlgorithm().getProperties() == null)
			throw new IllegalArgumentException();
		for (int i = 0; i < getSelectedAlgorithm().getProperties().length; i++)
			if (getSelectedAlgorithm().getProperties()[i] == p)
				return cards.get(listSelectedAlgorithm.toString()).getComponentForProperty(p);
		throw new IllegalArgumentException();
	}

	@Override
	public void proceed()
	{
		algProvider.storeListAlgorithmToProps(listSelectedAlgorithm);
		PropHandler.storeProperties();
	}

	private Messages msg;

	public void update(DatasetFile dataset, FeatureInfo featureInfo, DatasetClusterer clusterer)
	{
		getSelectedAlgorithm().update(dataset);
		if (!binaryFound)
			msg = Messages.errorMessage(""); // error is obvious (there is a error icon in the binary panel)
		else
			msg = getSelectedAlgorithm().getMessages(dataset, featureInfo, clusterer);
	}

	@Override
	public final Messages canProceed()
	{
		return msg;
	}

	public Algorithm getSelectedAlgorithm()
	{
		return listSelectedAlgorithm;
	}
}
