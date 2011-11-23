package gui;

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
import java.util.ArrayList;
import java.util.List;

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

import main.Settings;
import util.ArrayUtil;
import util.CountedSet;
import util.DefaultComparator;
import util.ImageLoader;
import util.StringUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import data.DatasetFile;
import data.FeatureLoader;
import dataInterface.FragmentPropertySet;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import dataInterface.MoleculePropertySet;
import freechart.BarPlotPanel;
import freechart.HistogramPanel;

public class MoleculePropertyPanel extends JPanel
{
	public static final String PROPERTY_TYPE_CHANGED = "PROPERTY_TYPE_CHANGED";
	public static final String PROPERTY_CACHED_FEATURE_LOADED = "PROPERTY_CACHED_FEATURE_LOADED";

	private int selectedPropertyIndex = 0;
	private MoleculePropertySet selectedPropertySet;

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
	JComboBox comboBox = new JComboBox();
	JLabel missingValuesLabel = new JLabel(ImageLoader.WARNING);
	LinkButton rawDataLink = new LinkButton("Raw data...");
	JPanel featurePlotPanel = new JPanel(new BorderLayout());
	ButtonGroup radioGroup = new ButtonGroup();
	JToggleButton nominalFeatureButton = new JToggleButton("Nominal", ImageLoader.DISTINCT);
	JToggleButton numericFeatureButton = new JToggleButton("Numeric", ImageLoader.NUMERIC);
	JPanel mainPanel;

	private boolean selfUpdate = false;

	DatasetFile dataset;

	JPanel fragmentProps;

	public MoleculePropertyPanel(FeatureWizardPanel featurePanel)
	{
		fragmentProps = featurePanel.getFragmentPropPanel().getSummaryPanel();
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
		b3.append(Settings.getBinaryComponent(Settings.BABEL_BINARY));
		babelBinaryPanel = b3.getPanel();
		cardPanel.add(babelBinaryPanel, "babel-binary");

		//		SwingUtil.setDebugBorder(this, Color.RED);
		//		SwingUtil.setDebugBorder(descPanel, Color.CYAN);
		//		SwingUtil.setDebugBorder(fragmentProps, Color.YELLOW);
		//		SwingUtil.setDebugBorder(cardPanel, Color.BLUE);

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

				if (selectedPropertySet != null
						&& selectedPropertySet.get(dataset, selectedPropertyIndex).getType() != type
						&& selectedPropertySet.get(dataset, selectedPropertyIndex).isTypeAllowed(type))
				{
					selectedPropertySet.get(dataset, selectedPropertyIndex).setType(type);
					firePropertyChange(PROPERTY_TYPE_CHANGED, false, true);
					loadComputedOrCachedProperty();
				}
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
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
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
						(Window) MoleculePropertyPanel.this.getTopLevelAncestor());
			}
		});

		FeatureLoader.instance.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				setSelectedPropertySet(selectedPropertySet);
			}
		});

		rawDataLink.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				JOptionPane.showMessageDialog(Settings.TOP_LEVEL_COMPONENT, new JScrollPane(rawDataTable()));
			}
		});
	}

	private JTable rawDataTable()
	{
		MoleculeProperty selectedProperty = selectedPropertySet.get(dataset, selectedPropertyIndex);
		Object o[];
		Double n[] = null;
		String[] c = new String[] { "Index", selectedPropertySet.get(dataset, selectedPropertyIndex).toString() };
		if (selectedProperty.getType() == Type.NUMERIC)
		{
			o = selectedProperty.getDoubleValues(dataset);
			n = selectedProperty.getNormalizedValues(dataset);
			c = ArrayUtil.concat(c, new String[] { selectedPropertySet.get(dataset, selectedPropertyIndex).toString()
					+ " normalized" });
		}
		else
			o = selectedProperty.getStringValues(dataset);

		Object v[][] = new Object[o.length][selectedProperty.getType() == Type.NUMERIC ? 3 : 2];
		for (int i = 0; i < o.length; i++)
		{
			v[i][0] = new Integer(i);
			v[i][1] = o[i];
			if (selectedProperty.getType() == Type.NUMERIC)
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
					value = StringUtil.formatDouble((Double) value, 3);
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		});
		TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(table.getModel());
		sorter.setComparator(0, new DefaultComparator<Integer>());
		if (selectedProperty.getType() == Type.NUMERIC)
		{
			sorter.setComparator(1, new DefaultComparator<Double>());
			sorter.setComparator(2, new DefaultComparator<Double>());
		}
		else
			sorter.setComparator(1, new DefaultComparator<String>());
		table.setRowSorter(sorter);
		return table;
	}

	private void updatePlotPanel(MoleculePropertySet prop, int propIndex)
	{
		selfUpdate = true;
		comboBox.removeAllItems();

		for (int i = 0; i < prop.getSize(dataset); i++)
			comboBox.addItem(prop.get(dataset, i));
		if (prop.getSize(dataset) > 0)
		{
			comboBox.setSelectedIndex(propIndex);
			if (prop.get(dataset, propIndex).isSmartsProperty())
				comboBox.setToolTipText(prop.get(dataset, propIndex).getSmarts());
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
			MoleculeProperty selectedProperty = prop.get(dataset, propIndex);
			nominalFeatureButton.setEnabled(selectedProperty.isTypeAllowed(Type.NOMINAL));
			numericFeatureButton.setEnabled(selectedProperty.isTypeAllowed(Type.NUMERIC));

			if (prop.get(dataset, propIndex).numMissingValues(dataset) > 0)
			{
				missingValuesLabel.setVisible(true);
				missingValuesLabel.setText("Missing values: " + prop.get(dataset, propIndex).numMissingValues(dataset));
			}
			else
				missingValuesLabel.setVisible(false);

			rawDataLink.setEnabled(true);

			MoleculeProperty.Type type = selectedProperty.getType();
			if (type == Type.NOMINAL)
			{
				nominalFeatureButton.setSelected(true);
				CountedSet<String> set = CountedSet.fromArray(selectedProperty.getStringValues(dataset));
				List<String> values = set.values(new DefaultComparator<String>());
				if (values.contains(null))
					values.remove(null);
				List<Double> counts = new ArrayList<Double>();
				for (String o : values)
					if (o != null)
						counts.add((double) set.getCount(o));
				p = new BarPlotPanel(null, "#compounds", counts, values);
				p.setOpaque(false);
				p.setPreferredSize(new Dimension(300, 180));

			}
			else if (type == Type.NUMERIC)
			{
				numericFeatureButton.setSelected(true);
				List<Double> vals = ArrayUtil.removeNullValues(selectedProperty.getDoubleValues(dataset));
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
					p.setOpaque(false);
					p.setPreferredSize(new Dimension(300, 180));
				}
			}
			else
			{
				CountedSet<String> set = CountedSet.fromArray(selectedProperty.getStringValues(dataset));
				p = new MessageLabel(Message.warningMessage("This feature ('" + selectedProperty.toString()
						+ "') is most likely not suited for clustering and embedding.\n"
						+ "It is not numeric, and it has '" + set.size() + "' distinct values."));
				((MessageLabel) p).setMessageFont(((MessageLabel) p).getMessageFont().deriveFont(Font.BOLD));
				p.setBorder(new EmptyBorder(0, 10, 0, 0));
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

	private void loadComputedOrCachedProperty()
	{
		if (selectedPropertySet == null
				|| (!selectedPropertySet.isComputed(dataset) && !selectedPropertySet.isCached(dataset)))
			throw new Error("WTF");

		final MoleculePropertySet prop = selectedPropertySet;
		final int propIndex = selectedPropertyIndex;

		Thread th = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				synchronized (dataset)
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
				}
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

	public void setSelectedPropertySet(MoleculePropertySet prop)
	{
		System.out.println("updating selected prop: " + prop);

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
				if (prop.getBinary() != Settings.BABEL_BINARY)
					throw new Error("implement properly");
				showCard("babel-binary", babelBinaryPanel);
				fragmentProps.setVisible(false);
			}
			else
			{
				fragmentProps.setVisible(selectedPropertySet instanceof FragmentPropertySet);
				if (prop.isComputed(dataset) || prop.isCached(dataset))
					loadComputedOrCachedProperty();
				else
					showCard("loadButton", loadButtonPanel);
			}
		}
	}

	public MoleculePropertySet getSelectedPropertySet()
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
