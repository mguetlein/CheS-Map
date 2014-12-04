package org.chesmapper.map.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.FeatureLoader;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertySet;
import org.chesmapper.map.dataInterface.FragmentProperty;
import org.chesmapper.map.dataInterface.FragmentPropertySet;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.dataInterface.CompoundPropertySet.Type;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.ScreenSetup;
import org.chesmapper.map.main.Settings;
import org.mg.javalib.freechart.AbstractFreeChartPanel;
import org.mg.javalib.freechart.BarPlotPanel;
import org.mg.javalib.freechart.HistogramPanel;
import org.mg.javalib.gui.LinkButton;
import org.mg.javalib.gui.Message;
import org.mg.javalib.gui.MessageLabel;
import org.mg.javalib.gui.MoreTextPanel;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.CountedSet;
import org.mg.javalib.util.DefaultComparator;
import org.mg.javalib.util.ImageLoader;
import org.mg.javalib.util.StringUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class CompoundPropertyPanel extends JPanel
{
	public static final String PROPERTY_TYPE_CHANGED = "PROPERTY_TYPE_CHANGED";
	public static final String PROPERTY_CACHED_FEATURE_LOADED = "PROPERTY_CACHED_FEATURE_LOADED";

	private int selectedPropertyIndex = 0;
	private CompoundPropertySet selectedPropertySet;

	MoreTextPanel descPanel = new MoreTextPanel();
	JPanel cardPanel = new JPanel(new CardLayout());

	public static String LOAD_FEATURE_VALUES = "Load feature values";
	JButton loadButton = new JButton(LOAD_FEATURE_VALUES);
	JPanel loadingPanel;
	JPanel loadButtonPanel;
	JPanel babelBinaryPanel;

	//	JLabel loadingLabel = new JLabel("Loading feature...");

	JPanel comboPanel = new JPanel();
	JLabel comboLabel = new JLabel();
	JComboBox<CompoundProperty> comboBox = new JComboBox<CompoundProperty>();
	JLabel missingValuesLabel = new JLabel(ImageLoader.getImage(ImageLoader.Image.warning));
	LinkButton rawDataLink = new LinkButton("Raw data...");
	JPanel featurePlotPanel = new JPanel(new BorderLayout());
	ButtonGroup radioGroup = new ButtonGroup();
	JToggleButton nominalFeatureButton = new JToggleButton("Nominal", ImageLoader.getImage(ImageLoader.Image.distinct));
	JToggleButton numericFeatureButton = new JToggleButton("Numeric", ImageLoader.getImage(ImageLoader.Image.numeric));
	JPanel mainPanel;

	private boolean selfUpdate = false;
	DatasetFile dataset;
	JPanel fragmentProps;
	CheSMapperWizard wizard;

	public CompoundPropertyPanel(FeatureWizardPanel featurePanel, CheSMapperWizard wizard)
	{
		fragmentProps = featurePanel.getFragmentPropPanel().getSummaryPanel(wizard);
		this.wizard = wizard;
		buildLayout();
		addListeners();
	}

	private void buildLayout()
	{
		setLayout(new BorderLayout(10, 10));

		JPanel p = new JPanel(new BorderLayout(5, 5));
		descPanel.setPreferredWith(200);
		p.add(descPanel, BorderLayout.NORTH);
		p.add(fragmentProps, BorderLayout.SOUTH);
		fragmentProps.setVisible(false);

		add(p, BorderLayout.NORTH);
		add(cardPanel);

		mainPanel = new JPanel(new BorderLayout(10, 10));

		radioGroup.add(nominalFeatureButton);
		radioGroup.add(numericFeatureButton);
		DefaultFormBuilder radioBuilder = new DefaultFormBuilder(new FormLayout("p"));
		radioBuilder.append("Feature type:");
		radioBuilder.append(nominalFeatureButton);
		radioBuilder.append(numericFeatureButton);

		radioBuilder.append(missingValuesLabel);

		rawDataLink.setForegroundFont(rawDataLink.getFont().deriveFont(Font.PLAIN));
		rawDataLink.setSelectedForegroundFont(rawDataLink.getFont().deriveFont(Font.PLAIN));
		rawDataLink.setSelectedForegroundColor(Color.BLUE);
		radioBuilder.append(rawDataLink);

		DefaultFormBuilder comboBuilder = new DefaultFormBuilder(new FormLayout("p,3dlu,p"));
		comboBuilder.append(comboLabel);
		comboBuilder.append(comboBox);
		comboPanel = comboBuilder.getPanel();

		mainPanel.add(comboPanel, BorderLayout.NORTH);
		mainPanel.add(featurePlotPanel);
		mainPanel.add(radioBuilder.getPanel(), BorderLayout.WEST);

		cardPanel.add(mainPanel, "main");

		loadingPanel = new JPanel(new BorderLayout());
		loadingPanel.add(new JLabel("Loading..."));
		cardPanel.add(loadingPanel, "loading");

		DefaultFormBuilder b2 = new DefaultFormBuilder(new FormLayout("p"));
		b2.append(loadButton);
		loadButtonPanel = b2.getPanel();
		cardPanel.add(loadButtonPanel, "loadButton");

		DefaultFormBuilder b3 = new DefaultFormBuilder(new FormLayout("p"));
		b3.append(BinHandler.getBinaryComponent(BinHandler.BABEL_BINARY, (Window) getTopLevelAncestor()));
		babelBinaryPanel = b3.getPanel();
		cardPanel.add(babelBinaryPanel, "babel-binary");

		//		SwingUtil.setDebugBorder(this, Color.RED);
		//		SwingUtil.setDebugBorder(descPanel, Color.CYAN);
		//		SwingUtil.setDebugBorder(fragmentProps, Color.YELLOW);
		//		SwingUtil.setDebugBorder(cardPanel, Color.BLUE);

	}

	public void toggleType()
	{
		//		CompoundProperty p = selectedPropertySet.get(dataset, selectedPropertyIndex);
		if (selectedPropertySet.getType() == Type.NOMINAL)
			setType(Type.NUMERIC);
		else
			setType(Type.NOMINAL);
	}

	public void setType(Type type)
	{
		if (selectedPropertySet != null && selectedPropertySet.getType() != type
				&& selectedPropertySet.isTypeAllowed(type))
		{
			//			CompoundProperty p = selectedPropertySet.get(dataset, selectedPropertyIndex);
			selectedPropertySet.setType(type);
			firePropertyChange(PROPERTY_TYPE_CHANGED, false, true);
			loadComputedOrCachedProperty();
		}
	}

	private void addListeners()
	{
		ActionListener radioListener = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (selfUpdate)
					return;
				Type type = null;
				if (e.getSource() == nominalFeatureButton)
					type = Type.NOMINAL;
				else if (e.getSource() == numericFeatureButton)
					type = Type.NUMERIC;
				setType(type);
			}
		};
		nominalFeatureButton.addActionListener(radioListener);
		numericFeatureButton.addActionListener(radioListener);

		comboBox.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (selfUpdate)
					return;

				if (comboBox.getSelectedIndex() != selectedPropertyIndex)
				{
					selectedPropertyIndex = comboBox.getSelectedIndex();
					loadComputedOrCachedProperty();
				}
			}
		});
		comboBox.setRenderer(new DefaultListCellRenderer()
		{
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				String s = value + "";
				if (s.length() > 60)
					s = s.substring(0, 57) + "...";
				return super.getListCellRendererComponent(list, s, index, isSelected, cellHasFocus);
			}
		});

		loadButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				//load(true);
				showCard("loading", loadingPanel);
				FeatureLoader.instance.loadFeature(selectedPropertySet, dataset,
						(Window) CompoundPropertyPanel.this.getTopLevelAncestor());
			}
		});

		FeatureLoader.instance.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (wizard.isClosed())
					return;
				setSelectedPropertySet(selectedPropertySet);
			}
		});

		rawDataLink.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, new JScrollPane(rawDataTable()));
			}
		});
	}

	private JTable rawDataTable()
	{
		final CompoundProperty selectedProperty = selectedPropertySet.get(dataset, selectedPropertyIndex);
		Object o[];
		Double n[] = null;
		String[] c = new String[] { "Index", selectedPropertySet.get(dataset, selectedPropertyIndex).toString() };
		if (selectedPropertySet.getType() == Type.NUMERIC)
		{
			o = ((NumericProperty) selectedProperty).getDoubleValues();
			n = ((NumericProperty) selectedProperty).getNormalizedValues();
			c = ArrayUtil.concat(c, new String[] { selectedPropertySet.get(dataset, selectedPropertyIndex).toString()
					+ " normalized" });
		}
		else
			o = ((NominalProperty) selectedProperty).getStringValues();

		Object v[][] = new Object[o.length][selectedPropertySet.getType() == Type.NUMERIC ? 3 : 2];
		for (int i = 0; i < o.length; i++)
		{
			v[i][0] = new Integer(i + 1);
			v[i][1] = o[i];
			if (selectedPropertySet.getType() == Type.NUMERIC)
				v[i][2] = n[i];
		}
		DefaultTableModel model = new DefaultTableModel(v, c)
		{
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		JTable table = new JTable(model);
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
		{
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column)
			{
				if (value instanceof Double)
					if (column == 1 && ((NumericProperty) selectedProperty).isInteger())
						value = StringUtil.formatDouble((Double) value, 0);
					else
						value = StringUtil.formatDouble((Double) value, 3);
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		});
		TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(table.getModel());
		sorter.setComparator(0, new DefaultComparator<Integer>());
		if (selectedPropertySet.getType() == Type.NUMERIC)
		{
			sorter.setComparator(1, new DefaultComparator<Double>());
			sorter.setComparator(2, new DefaultComparator<Double>());
		}
		else
			sorter.setComparator(1, new DefaultComparator<String>());
		table.setRowSorter(sorter);
		return table;
	}

	private void updatePlotPanel(CompoundPropertySet prop, int propIndex)
	{
		selfUpdate = true;
		comboBox.removeAllItems();

		for (int i = 0; i < prop.getSize(dataset); i++)
			comboBox.addItem(prop.get(dataset, i));
		if (prop.getSize(dataset) > 0)
		{
			comboBox.setSelectedIndex(propIndex);
			if (prop.get(dataset, propIndex) instanceof FragmentProperty)
				comboBox.setToolTipText(((FragmentProperty) prop.get(dataset, propIndex)).getSmarts());
			else
				comboBox.setToolTipText("");
			comboBox.setEnabled(true);
		}
		else
			comboBox.setEnabled(false);

		if (prop.isSizeDynamic() || prop.getSize(dataset) > 1)
		{
			comboLabel.setText(prop + " has " + prop.getSize(dataset) + " features: ");
			comboPanel.setVisible(true);
		}
		else
			comboPanel.setVisible(false);

		featurePlotPanel.removeAll();
		radioGroup.clearSelection();

		JPanel p = null;
		if (prop.getSize(dataset) > 0)
		{
			nominalFeatureButton.setEnabled(selectedPropertySet.isTypeAllowed(Type.NOMINAL));
			numericFeatureButton.setEnabled(selectedPropertySet.isTypeAllowed(Type.NUMERIC));

			if (prop.get(dataset, propIndex).numMissingValues() > 0)
			{
				missingValuesLabel.setVisible(true);
				missingValuesLabel.setText("Missing values: " + prop.get(dataset, propIndex).numMissingValues());
			}
			else
				missingValuesLabel.setVisible(false);

			rawDataLink.setEnabled(true);

			CompoundPropertySet.Type type = selectedPropertySet.getType();
			if (type == Type.NOMINAL)
			{
				NominalProperty selectedProperty = (NominalProperty) prop.get(dataset, propIndex);
				nominalFeatureButton.setSelected(true);

				String values[] = new String[selectedProperty.getDomain().length];
				for (int i = 0; i < values.length; i++)
					values[i] = selectedProperty.getFormattedValue(selectedProperty.getDomain()[i]);
				int counts[] = selectedProperty.getDomainCounts();
				//				CountedSet<String> set = CountedSet.fromArray(selectedProperty.getStringValues(dataset));
				//				List<String> values = set.values(new DefaultComparator<String>());
				//				if (values.contains(null))
				//					values.remove(null);
				//				List<Double> counts = new ArrayList<Double>();
				//				for (String o : values)
				//					if (o != null)
				//						counts.add((double) set.getCount(o));
				//				for (int i = 0; i < values.size(); i++)
				//					if (selectedProperty.isSmartsProperty() || isExportedSmarts)
				//						values.set(i, AbstractFragmentProperty.getFormattedSmartsValue(values.get(i)));
				p = new BarPlotPanel(null, "#compounds", ArrayUtil.toPrimitiveDoubleArray(counts), values);
				((BarPlotPanel) p).setMaximumBarWidth(.35);
			}
			else if (type == Type.NUMERIC)
			{
				NumericProperty selectedProperty = (NumericProperty) prop.get(dataset, propIndex);
				numericFeatureButton.setSelected(true);
				List<Double> vals = ArrayUtil.removeNullValues(selectedProperty.getDoubleValues());

				//List<Double> vals = ArrayUtil.toList(selectedProperty.getNormalizedValues(dataset));

				if (vals.size() == 0)
				{
					p = new JPanel(new BorderLayout());
					p.add(new JLabel("Could not compute values for " + prop.get(dataset, propIndex).toString()));
				}
				else
				{
					p = new HistogramPanel(null, null, selectedProperty.toString(), "#compounds", "",
							ArrayUtil.toPrimitiveDoubleArray(vals), 20, null, true);
				}
			}
			else
			{
				NominalProperty selectedProperty = (NominalProperty) prop.get(dataset, propIndex);
				CountedSet<String> set = CountedSet.fromArray(selectedProperty.getStringValues());
				p = new MessageLabel(Message.warningMessage("This feature ('" + selectedProperty.toString()
						+ "') is most likely not suited for clustering and embedding.\n"
						+ "It is not numeric, and it has '" + set.getNumValues() + "' distinct values."));
				((MessageLabel) p).setMessageFont(((MessageLabel) p).getMessageFont().deriveFont(Font.BOLD));
				p.setBorder(new EmptyBorder(0, 10, 0, 0));
			}

			if (p instanceof AbstractFreeChartPanel)
			{
				((AbstractFreeChartPanel) p).setIntegerTickUnits();
				((AbstractFreeChartPanel) p).setFontSize(ScreenSetup.INSTANCE.getFontSize());
				p.setOpaque(false);
				p.setPreferredSize(new Dimension(300, 180));
			}
		}
		else
		{
			nominalFeatureButton.setEnabled(false);
			numericFeatureButton.setEnabled(false);
			rawDataLink.setEnabled(false);
			if (prop instanceof FragmentPropertySet)
				p = new MessageLabel(Message.warningMessage("No fragments found. Try to change fragment settings."));
			else
				p = new MessageLabel(Message.warningMessage("This feature has no values."));
			((MessageLabel) p).setMessageFont(((MessageLabel) p).getMessageFont().deriveFont(Font.BOLD));
			p.setBorder(new EmptyBorder(0, 10, 0, 0));
		}
		if (p != null)
		{
			featurePlotPanel.add(p, BorderLayout.NORTH);
			featurePlotPanel.revalidate();
			featurePlotPanel.repaint();
		}
		selfUpdate = false;
	}

	Set<DatasetFile> loading = new HashSet<DatasetFile>();

	private void loadComputedOrCachedProperty()
	{
		if (selectedPropertySet == null
				|| (!selectedPropertySet.isComputed(dataset) && !(Settings.CACHING_ENABLED && selectedPropertySet
						.isCached(dataset))))
			throw new Error("WTF");

		if (loading.contains(dataset))
			return;
		loading.add(dataset);

		final CompoundPropertySet prop = selectedPropertySet;
		final int propIndex = selectedPropertyIndex;

		Thread th = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				boolean featureValuesAvailable = true;
				if (!prop.isComputed(dataset))
				{
					showCard("loading", loadingPanel);
					featureValuesAvailable = prop.compute(dataset);
					firePropertyChange(PROPERTY_CACHED_FEATURE_LOADED, false, true);
				}
				if (featureValuesAvailable)
				{
					// reading from file if cached takes some time, check if selection is still up to date
					if (prop != selectedPropertySet || propIndex != selectedPropertyIndex)
						return;
					updatePlotPanel(prop, propIndex);
					// loading freechart stuff takes some time, check if selection is still up to date
					if (prop != selectedPropertySet || propIndex != selectedPropertyIndex)
						return;
					showCard("main", mainPanel);
				}
				else
					showCard("loadButton", loadButtonPanel);
				loading.remove(dataset);
			}
		});
		th.start();
	}

	private void showCard(String name, Component comp)
	{
		// HACK: jsplitpane and cardlayout dont quite like themselves,
		// the preferred size of the card-layout-component has to be decreased by hand
		//		descPanel.addParagraph("name: " + comp);
		//		descPanel.revalidate();
		//		descPanel.repaint();
		((CardLayout) cardPanel.getLayout()).show(cardPanel, name);
		cardPanel.setPreferredSize(comp.getPreferredSize());
		cardPanel.setVisible(true);
	}

	public void setSelectedPropertySet(CompoundPropertySet prop)
	{
		Settings.LOGGER.info("updating selected prop: " + prop);

		selectedPropertySet = prop;
		selectedPropertyIndex = 0;
		descPanel.clear();
		if (prop == null)
		{
			cardPanel.setVisible(false);
			fragmentProps.setVisible(false);
		}
		else
		{
			descPanel.addParagraph(prop.getDescription());

			if (prop.getBinary() != null && !prop.getBinary().isFound())
			{
				if (prop.getBinary() != BinHandler.BABEL_BINARY)
					throw new Error("implement properly");
				showCard("babel-binary", babelBinaryPanel);
				fragmentProps.setVisible(false);
			}
			else
			{
				fragmentProps.setVisible(selectedPropertySet instanceof FragmentPropertySet);
				if (prop.isComputed(dataset) || (Settings.CACHING_ENABLED && prop.isCached(dataset)))
					loadComputedOrCachedProperty();
				else
					showCard("loadButton", loadButtonPanel);
			}
		}
	}

	public CompoundPropertySet getSelectedPropertySet()
	{
		return selectedPropertySet;
	}

	public void setDataset(DatasetFile dataset)
	{
		this.dataset = dataset;
		cardPanel.setVisible(false);
	}

	public void showInfoText(String text)
	{
		selectedPropertySet = null;
		selectedPropertyIndex = -1;
		descPanel.clear();
		descPanel.addParagraph(text);
		cardPanel.setVisible(false);
	}
}
