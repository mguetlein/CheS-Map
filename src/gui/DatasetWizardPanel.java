package gui;

import io.SDFUtil;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Vector;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import main.Settings;
import opentox.DatasetUtil;
import util.ListUtil;
import util.SwingUtil;
import util.VectorUtil;
import alg.DatasetProvider;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

import data.DatasetFile;
import data.FeatureService.IllegalCompoundsException;

public class DatasetWizardPanel extends WizardPanel implements DatasetProvider
{
	JTextField textField;
	JFileChooser chooser;
	JList recentlyUsed;

	JButton buttonLoad;

	JLabel labelFile;
	JLabel labelSize;
	JLabel labelProps;
	JLabel labelNumericProps;
	JLabel label3D;

	WizardDialog wizard;

	boolean loading = false;

	Vector<DatasetFile> oldDatasets;

	private DatasetFile dataset;

	public DatasetWizardPanel(WizardDialog wizard)
	{
		this.wizard = wizard;
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(
				"pref,5dlu,pref:grow(0.99),5dlu,right:p:grow(0.01)"));

		builder.append(new JLabel(
				"Select dataset file (Copy a http link into the textfield to load a dataset from the internet):"), 3);
		builder.nextLine();

		textField = new JTextField(45);
		textField.getDocument().addDocumentListener(new DocumentAdapter()
		{
			@Override
			public void update(DocumentEvent e)
			{
				String text = textField.getText().trim();
				if (dataset != null && !dataset.getPath().equals(text))
				{
					dataset = null;
					updateDataset();
					buttonLoad.setEnabled(true);
				}
				else if (dataset == null)
					buttonLoad.setEnabled(true);
			}
		});
		textField.addKeyListener(new KeyAdapter()
		{
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					load(textField.getText());
				}
			}
		});
		builder.append(textField, 3);

		JButton search = new JButton("Open file...");
		search.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (chooser == null)
				{
					String dir = (String) Settings.PROPS.get("dataset-current-dir");
					if (dir == null)
						dir = System.getProperty("user.home");
					chooser = new JFileChooser(new File(dir));
				}
				chooser.showOpenDialog(DatasetWizardPanel.this.wizard);
				File f = chooser.getSelectedFile();
				if (f != null)
				{
					Settings.PROPS.put("dataset-current-dir", f.getParent());
					Settings.storeProps();
					//textField.setText(f.getPath());
					load(f.getPath());
				}
			}
		});
		builder.append(search);
		builder.nextLine();

		String dir = (String) Settings.PROPS.get("dataset-recently-used");
		oldDatasets = new Vector<DatasetFile>();
		if (dir != null)
		{
			Vector<String> strings = VectorUtil.fromCSVString(dir);
			for (String s : strings)
				oldDatasets.add(DatasetFile.fromString(s));
		}
		final DefaultListModel m = new DefaultListModel();
		for (DatasetFile d : oldDatasets)
			m.addElement(d);
		recentlyUsed = new JList(m);
		recentlyUsed.setFont(recentlyUsed.getFont().deriveFont(Font.PLAIN));
		recentlyUsed.setCellRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				DatasetFile d = (DatasetFile) value;
				String s = "<html><b>" + d.getName() + "</b><br>(" + d.getLocalPath() + ")</html>";
				setBorder(new EmptyBorder(0, 0, 3, 0));
				return super.getListCellRendererComponent(list, s, index, isSelected, cellHasFocus);
			}
		});
		recentlyUsed.setVisibleRowCount(7);
		recentlyUsed.addListSelectionListener(new ListSelectionListener()
		{

			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (recentlyUsed.getSelectedIndex() != -1)
				{
					//textField.setText(((Dataset) recentlyUsed.getSelectedValue()).getPath());
					load(((DatasetFile) recentlyUsed.getSelectedValue()).getPath());
				}

			}
		});
		recentlyUsed.addKeyListener(new KeyAdapter()
		{
			public void keyReleased(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_DELETE && recentlyUsed.getSelectedIndex() != -1)
				{
					oldDatasets.remove(recentlyUsed.getSelectedIndex());
					m.remove(recentlyUsed.getSelectedIndex());
				}
			}
		});

		//		builder.appendRow("top:pref:grow");

		JScrollPane scroll = new JScrollPane(recentlyUsed);
		HideablePanel hide = new HideablePanel("Recently used datasets", true);
		hide.setHorizontalAlignement(SwingConstants.RIGHT);
		hide.setBorder(new EmptyBorder(10, 0, 10, 0));
		hide.addComponent(scroll);
		builder.append(hide, 5);
		builder.nextLine();

		buttonLoad = new JButton("Load Dataset");
		buttonLoad.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				load(textField.getText());
			}
		});

		builder.append(ButtonBarFactory.buildRightAlignedBar(buttonLoad), 5);
		builder.nextLine();

		//		builder.appendParagraphGapRow();
		//		builder.nextLine();
		builder.appendSeparator("Dataset Properties");
		builder.nextLine();

		labelFile = new JLabel("-");
		labelSize = new JLabel("-");
		labelProps = new JLabel("-");
		labelNumericProps = new JLabel("-");
		label3D = new JLabel("-");

		builder.append("File:");
		builder.append(labelFile, 3);
		builder.nextLine();

		builder.append("Num compounds:");
		builder.append(labelSize, 3);
		builder.nextLine();

		builder.append("Num properties per compound:");
		builder.append(labelProps, 3);
		builder.nextLine();

		builder.append("3D available:");
		builder.append(label3D, 3);
		builder.nextLine();
		setLayout(new BorderLayout());
		add(builder.getPanel());

		// auto load last selected dataset
		if (oldDatasets.size() > 0)
			load(oldDatasets.get(0).getPath());
		//textField.setText(oldDatasets.get(0).getPath());
	}

	private void updateDataset()
	{
		if (dataset == null)
		{
			labelFile.setText("-");
			labelProps.setText("-");
			labelSize.setText("-");
			label3D.setText("-");
			recentlyUsed.clearSelection();
		}
		else
		{
			labelFile.setText(dataset.getName());
			labelProps.setText(dataset.getIntegratedProperties(true).length + "");
			labelSize.setText(dataset.numCompounds() + "");
			label3D.setText(dataset.has3D() + "");
			if (oldDatasets.indexOf(dataset) == -1)
				if (oldDatasets.contains(dataset))
					recentlyUsed.setSelectedValue(dataset, true);
				else
					recentlyUsed.clearSelection();
		}
		wizard.update();
	}

	//	public static void main(String args[])
	//	{
	//		System.out.println(loadHttpFile("http://apps.ideaconsult.net:8080/ambit2/dataset/272?max=5"));
	//	}

	//	private void load(DatasetFile d)
	//	{
	//		if (d.isLocal())
	//			load(d.getLocalPath());
	//		else
	//			load(d.getURI());
	//	}

	public void load(String f)
	{
		if (loading)
			return;
		loading = true;

		dataset = null;
		if (!textField.getText().equals(f))
			textField.setText(f);
		buttonLoad.setEnabled(false);

		final DatasetFile d;
		boolean http = f.startsWith("http");
		if (http)
			d = DatasetFile.getURLDataset(f);
		else
			d = DatasetFile.localFile(f);

		if (d.isLoaded())
		{
			dataset = d;
			updateDataset();
			loading = false;
		}
		else
		{
			SwingUtil.loadingLabel(labelFile);
			labelProps.setText("-");
			labelNumericProps.setText("-");
			labelSize.setText("-");
			label3D.setText("-");

			Thread th = new Thread(new Runnable()
			{
				public void run()
				{
					boolean urlDatasetDownloadFailed = false;
					if (!d.isLocal())
						urlDatasetDownloadFailed = !DatasetUtil.downloadDataset(d.getURI());

					if (!urlDatasetDownloadFailed)
					{
						File datasetFile = new File(d.getLocalPath());
						try
						{
							if (datasetFile != null && datasetFile.exists())
							{
								d.loadDataset();
								if (d.numCompounds() == 0)
									throw new Exception("No compounds in file");
								dataset = d;
							}
						}
						catch (IllegalCompoundsException e)
						{
							String cleanedSdf = Settings.destinationFile(d.getLocalPath(), d.getName() + ".cleaned");
							int res = JOptionPane.showConfirmDialog(
									Settings.TOP_LEVEL_COMPONENT,
									"Could not read " + e.illegalCompounds.size() + " compound/s in dataset: "
											+ d.getPath() + "\nIndices of compounds that could not be loaded: "
											+ ListUtil.toString(e.illegalCompounds)
											+ "\n\nDo you want to remove the faulty compounds and reload the dataset?"
											+ "\n(New dataset would be stored at: " + cleanedSdf + ")",
									"Dataset faulty", JOptionPane.YES_NO_OPTION);
							if (res == JOptionPane.YES_OPTION)
							{
								SDFUtil.filter_exclude(d.getSDFPath(false), cleanedSdf, e.illegalCompounds);
								loading = false;
								load(cleanedSdf);
								return;
							}
						}
						catch (Exception e)
						{
							e.printStackTrace();
							JOptionPane.showMessageDialog(
									Settings.TOP_LEVEL_COMPONENT,
									"Could not load local dataset:\n"
											+ d.getPath()
											+ "\nError: '"
											+ e.getMessage()
											+ "'\n\n(Make sure to start the dataset URL with 'http' if you want to load an external dataset.)",
									"Dataset could not be loaded", JOptionPane.ERROR_MESSAGE);
						}
					}
					if (dataset == null)
						buttonLoad.setEnabled(true);
					updateDataset();
					loading = false;
				}
			});
			th.start();
		}

	}

	@Override
	public void proceed()
	{
		if (oldDatasets.contains(dataset))
			oldDatasets.removeElement(dataset);
		oldDatasets.add(0, dataset);
		Vector<String> strings = new Vector<String>();
		for (DatasetFile d : oldDatasets)
			strings.add(d.toString());
		Settings.PROPS.put("dataset-recently-used", VectorUtil.toCSVString(strings));
		Settings.storeProps();
	}

	@Override
	public boolean canProceed()
	{
		return getDatasetFile() != null;
	}

	@Override
	public String getTitle()
	{
		return "Load Dataset";
	}

	@Override
	public DatasetFile getDatasetFile()
	{
		return dataset;
	}

	public DatasetProvider getDatasetProvider()
	{
		return this;
	}

	@Override
	public String getDescription()
	{
		return "Select a dataset from your file system for clustering, embedding and visualization.";
	}

}
