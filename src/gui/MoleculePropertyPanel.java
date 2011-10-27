package gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import main.Settings;
import main.TaskProvider;
import util.ArrayUtil;
import util.CountedSet;
import util.DefaultComparator;
import util.ImageLoader;
import util.StringUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import data.DatasetFile;
import dataInterface.FragmentPropertySet;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import dataInterface.MoleculePropertySet;
import freechart.BarPlotPanel;
import freechart.HistogramPanel;

public class MoleculePropertyPanel extends JPanel
{
	public static final String PROPERTY_TYPE_CHANGED = "PROPERTY_TYPE_CHANGED";
	public static final String PROPERTY_COMPUTED = "PROPERTY_COMPUTED";

	private int selectedPropertyIndex = 0;
	private MoleculePropertySet selectedPropertySet;

	MoreTextPanel descPanel = new MoreTextPanel();
	JPanel cardPanel = new JPanel(new CardLayout());

	JButton loadButton = new JButton("Load feature values");

	//	JLabel loadingLabel = new JLabel("Loading feature...");

	JPanel comboPanel = new JPanel();
	JLabel comboLabel = new JLabel();
	JComboBox comboBox = new JComboBox();
	LinkButton rawDataLink = new LinkButton("Raw data...");
	JPanel featurePlotPanel = new JPanel(new BorderLayout());
	ButtonGroup radioGroup = new ButtonGroup();
	JToggleButton nominalFeatureButton = new JToggleButton("Nominal", ImageLoader.DISTINCT);
	JToggleButton numericFeatureButton = new JToggleButton("Numeric", ImageLoader.NUMERIC);

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
		p.add(descPanel, BorderLayout.NORTH);
		p.add(fragmentProps, BorderLayout.SOUTH);
		fragmentProps.setVisible(false);

		add(p, BorderLayout.NORTH);
		add(cardPanel);

		JPanel main = new JPanel(new BorderLayout(10, 10));

		radioGroup.add(nominalFeatureButton);
		radioGroup.add(numericFeatureButton);
		DefaultFormBuilder radioBuilder = new DefaultFormBuilder(new FormLayout("p"));
		radioBuilder.append("Feature type:");
		radioBuilder.append(nominalFeatureButton);
		radioBuilder.append(numericFeatureButton);
		rawDataLink.setForegroundFont(rawDataLink.getFont().deriveFont(Font.PLAIN));
		rawDataLink.setSelectedForegroundFont(rawDataLink.getFont().deriveFont(Font.PLAIN));
		rawDataLink.setSelectedForegroundColor(Color.BLUE);
		radioBuilder.append(rawDataLink);

		DefaultFormBuilder comboBuilder = new DefaultFormBuilder(new FormLayout("p,3dlu,p"));
		comboBuilder.append(comboLabel);
		comboBuilder.append(comboBox);
		comboPanel = comboBuilder.getPanel();

		main.add(comboPanel, BorderLayout.NORTH);
		main.add(featurePlotPanel);
		main.add(radioBuilder.getPanel(), BorderLayout.WEST);

		cardPanel.add(main, "main");

		//		JPanel p = new JPanel(new BorderLayout());
		//		p.add(loadingLabel);
		//		cardPanel.add(p, "loading");

		DefaultFormBuilder b2 = new DefaultFormBuilder(new FormLayout("p"));
		b2.append(loadButton);
		cardPanel.add(b2.getPanel(), "loadButton");

		DefaultFormBuilder b3 = new DefaultFormBuilder(new FormLayout("p"));
		b3.append(Settings.getBinaryComponent(Settings.BABEL_BINARY));
		cardPanel.add(b3.getPanel(), "babel-binary");
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
					load(false);
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
					load(false);
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
				load(true);
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
		if (selectedProperty.getType() == Type.NUMERIC)
			o = selectedProperty.getDoubleValues(dataset);
		else
			o = selectedProperty.getStringValues(dataset);
		Double n[] = selectedProperty.getNormalizedValues(dataset);
		String[] c = new String[] { "Index", selectedPropertySet.get(dataset, selectedPropertyIndex).toString(),
				selectedPropertySet.get(dataset, selectedPropertyIndex).toString() + " normalized" };
		Object v[][] = new Object[o.length][3];
		for (int i = 0; i < o.length; i++)
		{
			v[i][0] = new Integer(i);
			v[i][1] = o[i];
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
			sorter.setComparator(1, new DefaultComparator<Double>());
		else
			sorter.setComparator(1, new DefaultComparator<String>());
		sorter.setComparator(2, new DefaultComparator<Double>());
		table.setRowSorter(sorter);
		return table;
	}

	private void load(boolean background)
	{
		//		if (background)
		//		{
		//			((CardLayout) cardPanel.getLayout()).show(cardPanel, "loading");
		//			cardPanel.setVisible(true);
		//		}

		Thread th = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				synchronized (dataset)
				{
					selfUpdate = true;
					comboBox.removeAllItems();

					MoleculePropertySet selectedPropertySet = MoleculePropertyPanel.this.selectedPropertySet;
					int selectedPropertyIndex = MoleculePropertyPanel.this.selectedPropertyIndex;
					boolean error = false;

					if (selectedPropertySet != null)
					{
						boolean loading = false;
						if (!selectedPropertySet.isComputed(dataset) || !cardPanel.isVisible())
						{
							loading = true;
							TaskProvider.registerThread("compute-features");
							cardPanel.add(TaskProvider.task().getPanel(), "computing-features");
							TaskProvider.task().update("Computing feature: " + selectedPropertySet + " ...");
							((CardLayout) cardPanel.getLayout()).show(cardPanel, "computing-features");
							cardPanel.setVisible(true);
						}

						// compute values first to know the number of values
						if (!selectedPropertySet.isComputed(dataset))
							error = !selectedPropertySet.compute(dataset);
						if (!error)
						{
							firePropertyChange(PROPERTY_COMPUTED, false, true);

							for (int i = 0; i < selectedPropertySet.getSize(dataset); i++)
								comboBox.addItem(selectedPropertySet.get(dataset, i));
							if (selectedPropertySet.getSize(dataset) > 0)
							{
								comboBox.setSelectedIndex(selectedPropertyIndex);
								if (selectedPropertySet.get(dataset, selectedPropertyIndex).isSmartsProperty())
									comboBox.setToolTipText(selectedPropertySet.get(dataset, selectedPropertyIndex)
											.getSmarts());
								else
									comboBox.setToolTipText("");
								comboBox.setEnabled(true);
							}
							else
								comboBox.setEnabled(false);

							if (selectedPropertySet.isSizeDynamic() || selectedPropertySet.getSize(dataset) > 1)
							{
								comboLabel.setText(selectedPropertySet + " has " + selectedPropertySet.getSize(dataset)
										+ " features: ");
								comboPanel.setVisible(true);
							}
							else
								comboPanel.setVisible(false);

							featurePlotPanel.removeAll();
							radioGroup.clearSelection();

							JPanel p = null;
							if (selectedPropertySet.getSize(dataset) > 0)
							{
								MoleculeProperty selectedProperty = selectedPropertySet.get(dataset,
										selectedPropertyIndex);
								nominalFeatureButton.setEnabled(selectedProperty.isTypeAllowed(Type.NOMINAL));
								numericFeatureButton.setEnabled(selectedProperty.isTypeAllowed(Type.NUMERIC));
								rawDataLink.setEnabled(true);

								MoleculeProperty.Type type = selectedProperty.getType();
								if (type == Type.NOMINAL)
								{
									nominalFeatureButton.setSelected(true);
									CountedSet<String> set = CountedSet.fromArray(selectedProperty
											.getStringValues(dataset));
									List<String> values = set.values(new DefaultComparator<String>());
									List<Double> counts = new ArrayList<Double>();
									for (String o : values)
										counts.add((double) set.getCount(o));
									p = new BarPlotPanel(null, "#compounds", counts, values);
									p.setOpaque(false);
									p.setPreferredSize(new Dimension(300, 180));

								}
								else if (type == Type.NUMERIC)
								{
									numericFeatureButton.setSelected(true);
									List<Double> vals = ArrayUtil.removeNullValues(selectedProperty
											.getDoubleValues(dataset));
									//List<Double> vals = ArrayUtil.toList(selectedProperty.getNormalizedValues(dataset));

									if (vals.size() == 0)
									{
										p = new JPanel(new BorderLayout());
										p.add(new JLabel("Could not compute values for "
												+ selectedPropertySet.get(dataset, selectedPropertyIndex).toString()));
									}
									else
									{
										p = new HistogramPanel(null, null, selectedProperty.toString(), "#compounds",
												"", ArrayUtil.toPrimitiveDoubleArray(vals), 20, null, true);
										p.setOpaque(false);
										p.setPreferredSize(new Dimension(300, 180));
									}
								}
								else
								{
									CountedSet<String> set = CountedSet.fromArray(selectedProperty
											.getStringValues(dataset));
									//				List<String> values = set.values();
									//				List<Double> counts = new ArrayList<Double>();
									//				for (String o : values)
									//					counts.add((double) set.getCount(o));
									String text = "This feature ('" + selectedProperty.toString()
											+ "') is most likely not suited for clustering and embedding.\n"
											+ "It is not numeric, and it has '" + set.size() + "' distinct values.";
									//				for (int i = 0; i < values.size(); i++)
									//					text += "(" + counts.get(i).intValue() + ") " + values.get(i) + "\n";

									JTextArea infoTextArea2 = new JTextArea(text);
									JLabel infoIcon = new JLabel(ImageLoader.WARNING);
									infoIcon.setVerticalAlignment(SwingConstants.TOP);
									infoTextArea2.setFont(infoTextArea2.getFont().deriveFont(Font.BOLD));
									infoTextArea2.setBorder(null);
									infoTextArea2.setEditable(false);
									infoTextArea2.setOpaque(false);
									infoTextArea2.setWrapStyleWord(true);
									infoTextArea2.setLineWrap(true);
									p = new JPanel(new BorderLayout(5, 0));
									p.add(infoIcon, BorderLayout.WEST);
									p.add(infoTextArea2);
									p.setBorder(new EmptyBorder(0, 10, 0, 0));
								}
							}
							else
							{
								nominalFeatureButton.setEnabled(false);
								numericFeatureButton.setEnabled(false);
								rawDataLink.setEnabled(false);

								String text = "This feature has no values.";
								JTextArea infoTextArea2 = new JTextArea(text);
								JLabel infoIcon = new JLabel(ImageLoader.WARNING);
								infoIcon.setVerticalAlignment(SwingConstants.TOP);
								infoTextArea2.setFont(infoTextArea2.getFont().deriveFont(Font.BOLD));
								infoTextArea2.setBorder(null);
								infoTextArea2.setEditable(false);
								infoTextArea2.setOpaque(false);
								infoTextArea2.setWrapStyleWord(true);
								infoTextArea2.setLineWrap(true);
								p = new JPanel(new BorderLayout(5, 0));
								p.add(infoIcon, BorderLayout.WEST);
								p.add(infoTextArea2);
								p.setBorder(new EmptyBorder(0, 10, 0, 0));
							}
							if (p != null)
							{
								featurePlotPanel.add(p);
								featurePlotPanel.revalidate();
								featurePlotPanel.repaint();
							}
						}

						if (loading)
						{
							if (TaskProvider.task().containsWarnings())
								TaskProvider.task().showWarningDialog(Settings.TOP_LEVEL_COMPONENT, "Error",
										"An error occured while this feature was computed:");
							TaskProvider.clear();
						}
					}
					selfUpdate = false;

					if (error)
						update(false);
					else if (selectedPropertySet == MoleculePropertyPanel.this.selectedPropertySet
							&& selectedPropertyIndex == MoleculePropertyPanel.this.selectedPropertyIndex)
						((CardLayout) cardPanel.getLayout()).show(cardPanel, "main");
				}
			}
		});
		th.start();
		if (!background)
		{
			try
			{
				th.join();
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void update(boolean load)
	{
		if (selectedPropertySet == null)
		{
			cardPanel.setVisible(false);
			fragmentProps.setVisible(false);
		}
		else
		{
			fragmentProps.setVisible(selectedPropertySet instanceof FragmentPropertySet);

			if (load)
				load(true);
			else
			{
				((CardLayout) cardPanel.getLayout()).show(cardPanel, "loadButton");
				cardPanel.setVisible(true);
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

	public void setSelectedPropertySet(MoleculePropertySet prop)
	{
		selectedPropertySet = prop;
		selectedPropertyIndex = 0;
		descPanel.clear();
		if (prop != null)
			descPanel.addParagraph(prop.getDescription());

		if (prop != null && prop.getBinary() != null && !prop.getBinary().isFound())
		{
			if (prop.getBinary() != Settings.BABEL_BINARY)
				throw new Error("implement properly");
			((CardLayout) cardPanel.getLayout()).show(cardPanel, "babel-binary");
			cardPanel.setVisible(true);
			fragmentProps.setVisible(false);
		}
		else
			update(prop != null && prop.isComputed(dataset));
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
