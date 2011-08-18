package gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import main.Settings;
import util.ArrayUtil;
import util.CountedSet;
import util.ImageLoader;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import data.DatasetFile;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import dataInterface.MoleculePropertySet;
import freechart.BarPlotPanel;
import freechart.HistogramPanel;

public class MoleculePropertyPanel extends JPanel
{
	public static final String PROPERTY_TYPE_CHANGED = "PROPERTY_TYPE_CHANGED";

	private int selectedPropertyIndex = 0;
	private MoleculePropertySet selectedPropertySet;

	JButton loadButton = new JButton("Load feature values");

	JLabel loading = new JLabel("Loading feature...");

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

	public MoleculePropertyPanel()
	{
		buildLayout();
		addListeners();
	}

	private void buildLayout()
	{
		JPanel main = new JPanel(new BorderLayout(10, 10));

		radioGroup.add(nominalFeatureButton);
		radioGroup.add(numericFeatureButton);
		DefaultFormBuilder radioBuilder = new DefaultFormBuilder(new FormLayout("p"));
		radioBuilder.append("Feature type:");
		radioBuilder.append(nominalFeatureButton);
		radioBuilder.append(numericFeatureButton);
		radioBuilder.append(rawDataLink);

		DefaultFormBuilder comboBuilder = new DefaultFormBuilder(new FormLayout("p,3dlu,p"));
		comboBuilder.append(comboLabel);
		comboBuilder.append(comboBox);
		comboPanel = comboBuilder.getPanel();

		main.add(comboPanel, BorderLayout.NORTH);
		main.add(featurePlotPanel);
		main.add(radioBuilder.getPanel(), BorderLayout.WEST);

		setLayout(new CardLayout());
		add(main, "main");

		DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("p"));
		b.append(loading);
		add(b.getPanel(), "loading");

		DefaultFormBuilder b2 = new DefaultFormBuilder(new FormLayout("p"));
		b2.append(loadButton);
		add(b2.getPanel(), "loadButton");
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

				if (selectedPropertySet != null && selectedPropertySet.get(selectedPropertyIndex).getType() != type
						&& selectedPropertySet.get(selectedPropertyIndex).isTypeAllowed(type))
				{
					selectedPropertySet.get(selectedPropertyIndex).setType(type);
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
				MoleculeProperty selectedProperty = selectedPropertySet.get(selectedPropertyIndex);
				Object o[];
				if (selectedProperty.getType() == Type.NUMERIC)
					o = dataset.getDoubleValues(selectedProperty);
				else
					o = dataset.getStringValues(selectedProperty);
				String[] c = new String[] { selectedPropertySet.toString() };
				Object v[][] = new Object[o.length][1];
				for (int i = 0; i < o.length; i++)
					v[i][0] = o[i];
				JTable t = new JTable(v, c);
				JOptionPane.showMessageDialog(Settings.TOP_LEVEL_COMPONENT, new JScrollPane(t));
			}
		});
	}

	private void load(boolean background)
	{
		if (background)
		{
			((CardLayout) getLayout()).show(this, "loading");
			setVisible(true);
		}

		Thread th = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				synchronized (dataset)
				{
					selfUpdate = true;
					comboBox.removeAllItems();

					MoleculePropertySet currentSet = selectedPropertySet;
					int currentIndex = selectedPropertyIndex;

					if (selectedPropertySet != null)
					{
						MoleculeProperty selectedProperty = selectedPropertySet.get(selectedPropertyIndex);

						for (int i = 0; i < selectedPropertySet.getSize(); i++)
							comboBox.addItem(selectedPropertySet.get(i));
						comboBox.setSelectedIndex(selectedPropertyIndex);

						if (selectedPropertySet.getSize() > 1)
						{
							comboLabel.setText(selectedPropertySet + " has " + selectedPropertySet.getSize()
									+ " features: ");
							comboPanel.setVisible(true);
						}
						else
							comboPanel.setVisible(false);

						featurePlotPanel.removeAll();
						radioGroup.clearSelection();
						nominalFeatureButton.setEnabled(selectedProperty.isTypeAllowed(Type.NOMINAL));
						numericFeatureButton.setEnabled(selectedProperty.isTypeAllowed(Type.NUMERIC));

						JPanel p = null;
						MoleculeProperty.Type type = selectedProperty.getType();
						if (type == Type.NOMINAL)
						{
							nominalFeatureButton.setSelected(true);
							CountedSet<String> set = CountedSet.fromArray(dataset.getStringValues(selectedProperty));
							List<String> values = set.values();
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
							p = new HistogramPanel(null, null, selectedProperty.toString(), "#compounds", "",
									ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(dataset
											.getDoubleValues(selectedProperty))), 20, null, true);
							p.setOpaque(false);
							p.setPreferredSize(new Dimension(300, 180));
						}
						else
						{
							CountedSet<String> set = CountedSet.fromArray(dataset.getStringValues(selectedProperty));
							//				List<String> values = set.values();
							//				List<Double> counts = new ArrayList<Double>();
							//				for (String o : values)
							//					counts.add((double) set.getCount(o));
							String text = "This feature ('" + selectedProperty.toString()
									+ "') is most likely not suited for clustering and embedding.\n"
									+ "It is not numeric, and it has '" + set.size() + "' distinct values.";
							//				for (int i = 0; i < values.size(); i++)
							//					text += "(" + counts.get(i).intValue() + ") " + values.get(i) + "\n";

							JTextArea infoTextArea = new JTextArea(text);
							JLabel infoIcon = new JLabel(ImageLoader.WARNING);
							infoIcon.setVerticalAlignment(SwingConstants.TOP);
							infoTextArea.setFont(infoTextArea.getFont().deriveFont(Font.BOLD));
							infoTextArea.setBorder(null);
							infoTextArea.setEditable(false);
							infoTextArea.setOpaque(false);
							infoTextArea.setWrapStyleWord(true);
							infoTextArea.setLineWrap(true);
							p = new JPanel(new BorderLayout(5, 0));
							p.add(infoIcon, BorderLayout.WEST);
							p.add(infoTextArea);
							p.setBorder(new EmptyBorder(0, 10, 0, 0));
						}
						if (p != null)
						{
							featurePlotPanel.add(p);
						}
					}
					selfUpdate = false;

					if (currentSet == selectedPropertySet && currentIndex == selectedPropertyIndex)
						((CardLayout) getLayout()).show(MoleculePropertyPanel.this, "main");
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
			setVisible(false);
		else
		{
			if (load)
				load(true);
			else
			{
				((CardLayout) getLayout()).show(this, "loadButton");
				setVisible(true);
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
		setVisible(false);
	}

	public void setSelectedPropertySet(MoleculePropertySet prop)
	{
		selectedPropertySet = prop;
		selectedPropertyIndex = 0;
		update(prop != null && dataset.isComputed(prop));
	}
}
