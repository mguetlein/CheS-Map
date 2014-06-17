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
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import main.PropHandler;
import main.Settings;
import main.TaskProvider;
import opentox.DatasetUtil;
import task.Task;
import task.TaskDialog;
import util.FileUtil;
import util.ListUtil;
import util.StringUtil;
import util.ThreadUtil;
import util.VectorUtil;
import workflow.DatasetMappingWorkflowProvider;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

import connect.ChEMBLSearch;
import data.DatasetFile;
import data.FeatureService.IllegalCompoundsException;

public class DatasetWizardPanel extends WizardPanel implements DatasetMappingWorkflowProvider, DatasetLoader
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
	JComboBox<Boolean> comboBoxBigData;

	WizardDialog wizard;
	Blockable block;
	Vector<DatasetFile> oldDatasets;

	private DatasetFile dataset;
	private boolean showLoadDialog;

	private String propKeyDataset = "dataset-recently-used";

	private String propKeyBigData = "big-data-mode";

	public DatasetWizardPanel()
	{
		this(null, false);
	}

	public DatasetWizardPanel(boolean showLoadDialog)
	{
		this(null, showLoadDialog);
	}

	public DatasetWizardPanel(WizardDialog wizard)
	{
		this(wizard, true);
	}

	private DatasetWizardPanel(WizardDialog wizard, boolean showLoadDialog)
	{
		this.wizard = wizard;
		this.showLoadDialog = showLoadDialog;
		if (wizard == null)
		{
			block = new BlockableImpl();
			return;
		}

		block = wizard;
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(
				"pref,5dlu,pref:grow(0.99),5dlu,right:p:grow(0.01),5dlu,right:p:grow(0.01)"));
		int allCols = 7;
		builder.append(new JLabel(
				"Select dataset file (Copy a http link into the textfield to load a dataset from the internet):"),
				allCols);
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
					String dir = PropHandler.get("dataset-current-dir");
					if (dir == null)
						dir = System.getProperty("user.home");
					chooser = new JFileChooser(new File(dir));
				}
				chooser.showOpenDialog(DatasetWizardPanel.this.wizard);
				File f = chooser.getSelectedFile();
				if (f != null)
				{
					PropHandler.put("dataset-current-dir", f.getParent());
					PropHandler.storeProperties();
					//textField.setText(f.getPath());
					load(f.getPath());
				}
			}
		});
		builder.append(search);

		JButton chemblSearch = new JButton("Search ChEMBL");
		ActionListener al = new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				String f = Settings.destinationFile("chembl-search");
				ChEMBLSearch.searchDialog(DatasetWizardPanel.this.wizard, f, DatasetWizardPanel.this, true);
			}
		};
		chemblSearch.addActionListener(al);
		builder.append(chemblSearch);
		builder.nextLine();

		String dir = PropHandler.get(propKeyDataset);
		oldDatasets = new Vector<DatasetFile>();
		if (dir != null)
		{
			List<String> strings = StringUtil.split(dir);
			for (String s : strings)
				if (s != null && s.trim().length() > 0)
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
				String s = "<html><b>" + d.getFullName() + "</b><br>(" + d.getLocalPath() + ")</html>";
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
		builder.append(hide, allCols);
		builder.nextLine();

		buttonLoad = new JButton("Load Dataset");
		buttonLoad.setEnabled(false);
		buttonLoad.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				load(textField.getText());
			}
		});

		builder.append(ButtonBarFactory.buildRightAlignedBar(buttonLoad), allCols);
		builder.nextLine();

		//		builder.appendParagraphGapRow();
		//		builder.nextLine();
		JLabel l = new JLabel("Dataset Properties:");
		l.setFont(l.getFont().deriveFont(Font.BOLD));
		builder.append(l);
		builder.nextLine();

		labelFile = new JLabel("-");
		labelSize = new JLabel("-");
		labelProps = new JLabel("-");
		labelNumericProps = new JLabel("-");
		label3D = new JLabel("-");
		comboBoxBigData = new JComboBox<Boolean>(new Boolean[] { false, true });
		comboBoxBigData.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				String s;
				if (((Boolean) value))
					s = "No, show only data points.";
				else
					s = "Yes (default).";
				return super.getListCellRendererComponent(list, s, index, isSelected, cellHasFocus);
			}
		});
		comboBoxBigData.setEnabled(false);
		Settings.BIG_DATA = false;
		String selected = PropHandler.get(propKeyBigData);
		if (selected != null && selected.equals("true"))
			Settings.BIG_DATA = true;
		comboBoxBigData.setSelectedItem(Settings.BIG_DATA);
		comboBoxBigData.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				updateDataset();
				Settings.BIG_DATA = (Boolean) comboBoxBigData.getSelectedItem();
			}
		});

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

		builder.append("");
		builder.nextLine();

		JLabel l2 = new JLabel("3D Viewer Settings:");
		l2.setFont(l2.getFont().deriveFont(Font.BOLD));
		builder.append(l2);
		builder.nextLine();

		builder.append("Show compound structures:");
		builder.append(comboBoxBigData, 3);
		builder.nextLine();
		setLayout(new BorderLayout());
		add(builder.getPanel());

		// auto load last selected dataset
		if (oldDatasets.size() > 0)
		{
			//			if (oldDatasets.get(0).isLocal())
			load(oldDatasets.get(0).getPath()); // auto-load local files
			//			else
			//				textField.setText(oldDatasets.get(0).getPath()); //remote file, do not auto-load
		}
	}

	private void updateDataset()
	{
		if (wizard != null)
		{
			if (dataset == null)
			{
				labelFile.setText("-");
				labelProps.setText("-");
				labelSize.setText("-");
				label3D.setText("-");
				comboBoxBigData.setEnabled(false);
				recentlyUsed.clearSelection();
			}
			else
			{
				labelFile.setText(dataset.getFullName());
				labelProps.setText(dataset.getIntegratedProperties().length + "");
				labelSize.setText(dataset.numCompounds() + "");
				label3D.setText(dataset.has3D() + "");
				if (oldDatasets.indexOf(dataset) == -1)
					if (oldDatasets.contains(dataset))
						recentlyUsed.setSelectedValue(dataset, true);
					else
						recentlyUsed.clearSelection();
				comboBoxBigData.setEnabled(true);
			}
			wizard.update();
		}
	}

	//	public static void main(String args[])
	//	{
	//		Settings.LOGGER.println(loadHttpFile("http://apps.ideaconsult.net:8080/ambit2/dataset/272?max=5"));
	//	}

	//	private void load(DatasetFile d)
	//	{
	//		if (d.isLocal())
	//			load(d.getLocalPath());
	//		else
	//			load(d.getURI());
	//	}

	private void load(String f)
	{
		load(f, false);
	}

	public void load(final String f, boolean wait)
	{
		if (block.isBlocked())
			return;
		block.block(f);

		dataset = null;
		if (wizard != null)
		{
			if (!textField.getText().equals(f))
				textField.setText(f);
			buttonLoad.setEnabled(false);
		}
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
			block.unblock(f);
		}
		else
		{
			if (wizard != null)
			{
				labelFile.setText("-");
				labelProps.setText("-");
				labelNumericProps.setText("-");
				labelSize.setText("-");
				label3D.setText("-");
			}

			Thread th = new Thread(new Runnable()
			{
				public void run()
				{
					final Task task = TaskProvider.initTask("Loading dataset file");

					if (showLoadDialog)
					{
						if (wizard != null && !wizard.isVisible())
						{
							Thread th = new Thread(new Runnable()
							{
								@Override
								public void run()
								{
									while (!wizard.isVisible())
										ThreadUtil.sleep(100);
									if (task.isRunning())
										new TaskDialog(task, wizard);
								}
							});
							th.start();
						}
						else
							new TaskDialog(task, Settings.TOP_LEVEL_FRAME_SCREEN);
					}
					TaskProvider.update("Load dataset: " + d.getName());
					try
					{
						if (!d.isLocal())
						{
							TaskProvider.debug("Downloading external dataset: " + d.getName());
							DatasetUtil.downloadDataset(d.getURI());
						}
						if (TaskProvider.isRunning())
						{
							File datasetFile = new File(d.getLocalPath());
							if (datasetFile != null && datasetFile.exists())
							{
								d.loadDataset();
								if (TaskProvider.isRunning())
								{
									if (d.numCompounds() == 0)
										throw new Exception("No compounds in file");
									dataset = d;
								}
							}
							else
								throw new Exception("file not found: " + datasetFile.getAbsolutePath());
						}
						task.finish();
						TaskProvider.removeTask();
						SwingUtilities.invokeLater(new Runnable()
						{
							@Override
							public void run()
							{
								if (dataset == null)
									buttonLoad.setEnabled(true);
								updateDataset();
								block.unblock(f);
							}
						});
					}
					catch (IllegalCompoundsException e)
					{
						// illegal compounds can only be handled in sdf or csv
						if (d.getFileExtension() == null || !d.getFileExtension().matches("(?i)(csv|sdf)"))
							throw new Error(e);
						Settings.LOGGER.error(e);
						task.cancel();
						TaskProvider.removeTask();

						//						String cleanedFile = Settings.destinationFile("cleaned." + d.getFileExtension());
						int res = JOptionPane.showConfirmDialog(
								Settings.TOP_LEVEL_FRAME,
								"Could not read "
										+ e.illegalCompounds.size()
										+ " compound/s in dataset: "
										+ d.getPath()
										+ "\nIndices of compounds that could not be loaded: "
										+ ListUtil.toString(e.illegalCompounds)
										+ "\n\nDo you want to skip the faulty compounds, store the correct compounds in a new file, and reload this new file?",
								"Dataset faulty", JOptionPane.YES_NO_OPTION);
						block.unblock(f);
						boolean loadCleaned = false;
						if (res == JOptionPane.YES_OPTION)
						{
							String parent = FileUtil.getParent(d.getLocalPath());
							String cleanedFile = parent + File.separator + d.getShortName() + "_cleaned."
									+ d.getFileExtension();
							JFileChooser fc = new JFileChooser(parent);
							fc.setSelectedFile(new File(cleanedFile));
							int res2 = fc.showSaveDialog(Settings.TOP_LEVEL_FRAME);
							if (res2 == JFileChooser.APPROVE_OPTION)
							{
								if (fc.getSelectedFile().exists())
								{
									int res3 = JOptionPane.showConfirmDialog(Settings.TOP_LEVEL_FRAME, "File '"
											+ fc.getSelectedFile().getAbsolutePath() + "' already exists. Overwrite?",
											"Overwrite existing file?", JOptionPane.YES_NO_OPTION);
									if (res3 == JOptionPane.YES_OPTION)
										loadCleaned = true;
								}
								else
									loadCleaned = true;
							}
							if (loadCleaned)
							{
								cleanedFile = fc.getSelectedFile().getAbsolutePath();
								if (d.getFileExtension().matches("(?i)sdf"))
									SDFUtil.filter_exclude(d.getSDF(), cleanedFile, e.illegalCompounds, false);
								else if (d.getFileExtension().matches("(?i)csv"))
								{
									String all = FileUtil.readStringFromFile(d.getLocalPath());
									StringBuffer cleaned = new StringBuffer();
									int count = 0;
									for (String line : all.split("\n"))
									{
										if (!e.illegalCompounds.contains(new Integer(count - 1)))
										{
											cleaned.append(line);
											cleaned.append("\n");
										}
										count += 1;
									}
									FileUtil.writeStringToFile(cleanedFile, cleaned.toString());
									ThreadUtil.sleep(1000);
								}
								load(cleanedFile);
							}
						}
						if (!loadCleaned || dataset == null)
							buttonLoad.setEnabled(true);
					}
					catch (Throwable e)
					{
						Settings.LOGGER.error(e);
						TaskProvider.failed(
								"Could not load dataset: " + d.getPath(),
								"<html>"
										+ e.getClass().getSimpleName()
										+ ": '"
										+ e.getMessage()
										+ "'"
										+ (f.startsWith("http") ? ""
												: "<br>(Make sure to start the dataset URL with 'http' if you want to load an external dataset.)")
										+ "</html>");
						TaskProvider.removeTask();
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								if (wizard != null && dataset == null)
									buttonLoad.setEnabled(true);
								block.unblock(f);
							}
						});
					}
				}
			});
			th.start();
		}

		if (wait)
		{
			if (showLoadDialog && SwingUtilities.isEventDispatchThread())
				throw new Error("Cannot wait within awt event thread when load dialog is shown");
			ThreadUtil.sleep(100);
			while (block.isBlocked())
				ThreadUtil.sleep(100);
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
		PropHandler.put(propKeyDataset, VectorUtil.toCSVString(strings));
		PropHandler.put(propKeyBigData, ((Boolean) comboBoxBigData.getSelectedItem()).toString());
		PropHandler.storeProperties();
	}

	@Override
	public Messages canProceed()
	{
		if (getDatasetFile() != null)
			if (getDatasetFile().numCompounds() >= 1000 && (!(Boolean) comboBoxBigData.getSelectedItem()))
				return Messages
						.slowMessage("CheS-Mapper shows all compound structures simultaneously in 3D space. If the software runs slowly on your machine with large datasets, try disabling 'Show compound structures'.");
			else
				return null;
		else
			return Messages.errorMessage(""); // error is obvious (select dataset!)
	}

	public DatasetFile getDatasetFile()
	{
		return dataset;
	}

	@Override
	public String getTitle()
	{
		return Settings.text("dataset.title");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("dataset.desc");
	}

	@Override
	public void exportDatasetToMappingWorkflow(String datasetPath, boolean bigDataMode, Properties props)
	{
		load(datasetPath, true);
		if (dataset != null)
		{
			props.put(propKeyDataset, dataset.toString());
			props.put(propKeyBigData, bigDataMode);
		}
	}

	@Override
	public void exportSettingsToMappingWorkflow(Properties props)
	{
		props.put(propKeyDataset, StringUtil.split(PropHandler.get(propKeyDataset)).get(0));
		props.put(propKeyBigData, PropHandler.get(propKeyBigData));
	}

	@Override
	public DatasetFile getDatasetFromMappingWorkflow(Properties props, boolean storeToSettings,
			String alternateDatasetDir)
	{
		DatasetFile df = DatasetFile.fromString(StringUtil.split((String) props.get(propKeyDataset)).get(0));
		if (df.isLocal() && !new File(df.getLocalPath()).exists())
		{
			String alternate = alternateDatasetDir == null ? df.getName() : alternateDatasetDir + File.separator
					+ df.getName();
			if (new File(alternate).exists())
				df = DatasetFile.localFile(alternate);
			else
			{
				int res = JOptionPane.showConfirmDialog(Settings.TOP_LEVEL_FRAME, "The dataset file '" + df.getName()
						+ "' that was specified in the workflow could not be found.\n(Neither at " + df.getLocalPath()
						+ ",\nnor at " + alternate + ")\n\nLoad this file from a different location?",
						"Dataset not found", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
				if (res != JOptionPane.YES_OPTION)
					return null;
				final String finalName = df.getName();
				JFileChooser fc = new JFileChooser(alternateDatasetDir);
				fc.setFileFilter(new FileFilter()
				{
					@Override
					public String getDescription()
					{
						return "Dataset file " + finalName;
					}

					@Override
					public boolean accept(File f)
					{
						return f.isDirectory() || f.getName().equals(finalName);
					}
				});
				int res2 = fc.showOpenDialog(Settings.TOP_LEVEL_FRAME);
				if (res2 != JFileChooser.APPROVE_OPTION)
					return null;
				File f = fc.getSelectedFile();
				if (f != null && f.exists() && f.getName().equals(finalName))
					df = DatasetFile.localFile(f.getAbsolutePath());
				else
					return null;
			}
		}
		if (storeToSettings)
		{
			String newVal = df.toString();
			String oldVals = PropHandler.get(propKeyDataset);
			if (oldVals != null)
			{
				Vector<String> vec = new Vector<String>(StringUtil.split(oldVals, true));
				int index = vec.indexOf(newVal);
				if (index != -1)
					vec.remove(index);
				vec.insertElementAt(newVal, 0);
				PropHandler.put(propKeyDataset, VectorUtil.toCSVString(vec));
			}
			else
				PropHandler.put(propKeyDataset, newVal);
		}
		load(df.getPath(), true);

		Settings.BIG_DATA = false;
		String selected = (String) props.get(propKeyBigData);
		if (selected != null && selected.equals("true"))
			Settings.BIG_DATA = true;
		if (storeToSettings)
			PropHandler.put(propKeyBigData, ((Boolean) Settings.BIG_DATA).toString());

		return dataset;
	}
}
