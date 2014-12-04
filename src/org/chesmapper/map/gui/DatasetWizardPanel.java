package org.chesmapper.map.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import org.chesmapper.map.connect.ChEMBLSearch;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.main.PropHandler;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.util.MessageUtil;
import org.chesmapper.map.workflow.DatasetLoader;
import org.mg.javalib.gui.DownArrowButton;
import org.mg.javalib.gui.Message;
import org.mg.javalib.gui.MessageLabel;
import org.mg.javalib.gui.Messages;
import org.mg.javalib.gui.WizardDialog;
import org.mg.javalib.gui.WizardPanel;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.ListUtil;
import org.mg.javalib.util.OSUtil;
import org.mg.javalib.util.StringUtil;
import org.mg.javalib.util.SwingUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class DatasetWizardPanel extends WizardPanel
{
	JFileChooser chooser;

	JLabel labelFile;
	JLabel labelPath;
	JLabel labelSize;
	JLabel labelProps;
	JLabel labelNumericProps;
	JLabel label3D;
	JComboBox<Boolean> comboBoxBigData;

	WizardDialog wizard;
	Vector<DatasetFile> oldDatasets;
	List<String> oldWorkflows;

	private DatasetFile dataset;

	private DatasetLoader datasetLoader = new DatasetLoader(true);

	public DatasetWizardPanel(WizardDialog wizard)
	{
		this.wizard = wizard;
		if (wizard == null)
			throw new IllegalArgumentException("not yet supported anymore");

		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(
				"pref,5dlu,pref:grow(0.99),5dlu,right:p:grow(0.01),5dlu,right:p:grow(0.01)"));
		int allCols = 7;

		JLabel l = new JLabel("Select Dataset:");
		l.setFont(l.getFont().deriveFont(Font.BOLD));
		builder.append(l, allCols);
		builder.nextLine();

		//				"Select dataset file (Copy a http link into the textfield to load a dataset from the internet):"),

		DownArrowButton search = new DownArrowButton("Open file");
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
					JPanel p = new JPanel(new BorderLayout());
					MessageLabel m = new MessageLabel(
							Message.infoMessage(
									"To load a web file, paste URL into text field. Many chemical dataset formats supported (e.g., SDF, mol, cml, smi).",
									MessageUtil.createURLAction(Settings.HOMEPAGE_FORMATS)));
					m.setBorder(new EmptyBorder(5, 5, 5, 5));
					p.add(m, BorderLayout.NORTH);
					chooser.setAccessory(p);
				}
				int ret = chooser.showOpenDialog(DatasetWizardPanel.this.wizard);
				if (ret == JFileChooser.APPROVE_OPTION)
				{
					File f = chooser.getSelectedFile();
					if (f != null)
					{
						String p = f.getAbsolutePath();
						if (p.contains("http:"))
						{
							p = p.substring(p.indexOf("http:"));
							if (!p.startsWith("http://"))
							{
								if (OSUtil.isWindows())
									p = p.replaceAll("\\\\", "/");
								if (!p.startsWith("http://"))
									p = p.replaceFirst("http:/", "http://");
							}
							load(p);
						}
						else
						{
							if (f.getParent() != null)
							{
								PropHandler.put("dataset-current-dir", f.getParent());
								PropHandler.storeProperties();
							}
							//textField.setText(f.getPath());

							if (FileUtil.getFilenamExtension(f.getAbsolutePath()).matches("(?i)ches"))
								importFile(f);
							else
								load(f.getPath());
						}
					}
				}
			}
		});

		DownArrowButton importButton = new DownArrowButton("Import wizard settings");
		importButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				doImport();
			}
		});

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

		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		p.setBorder(new EmptyBorder(0, -10, 10, 0));
		p.add(search);
		p.add(importButton);
		p.add(chemblSearch);
		builder.append(p, allCols);
		builder.nextLine();

		oldDatasets = datasetLoader.loadRecentlyLoadedDatasets();
		JPopupMenu recentlyP = new JPopupMenu();
		for (final DatasetFile d : oldDatasets)
		{
			//String s = "<html><b>" + d.getFullName() + "</b><br><span style=\"font-size:75%\">(" + d.getLocalPath()
			String s = "<html><b>" + d.getName() + "</b><br><span style=\"font-size:75%\">(" + d.getPath()
					+ ")</span></html>";
			JMenuItem i = new JMenuItem(s);
			i.setFont(i.getFont().deriveFont(Font.PLAIN));
			i.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					load(d.getPath());
				}
			});
			recentlyP.add(i);
		}
		search.setPopupMenu(recentlyP);

		String dir = PropHandler.get("workflow-list");
		if (dir != null)
			oldWorkflows = StringUtil.split(dir);
		else
			oldWorkflows = new ArrayList<String>();
		JPopupMenu recentlyW = new JPopupMenu();
		for (final String w : oldWorkflows)
		{
			String s = "<html><b>" + FileUtil.getFilename(w) + "</b><br><span style=\"font-size:75%\">(" + w
					+ ")</span></html>";
			JMenuItem i = new JMenuItem(s);
			i.setFont(i.getFont().deriveFont(Font.PLAIN));
			i.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					importFile(new File(w));
				}
			});
			recentlyW.add(i);
		}
		importButton.setPopupMenu(recentlyW);

		//		builder.appendRow("top:pref:grow");

		//		builder.appendParagraphGapRow();
		//		builder.nextLine();
		l = new JLabel("Dataset Properties:");
		l.setFont(l.getFont().deriveFont(Font.BOLD));
		builder.append(l);
		builder.nextLine();

		labelFile = new JLabel("-");
		labelPath = new JLabel("-");
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
					s = Settings.text("dataset.big-data.enabled");
				else
					s = Settings.text("dataset.big-data.disabled");
				return super.getListCellRendererComponent(list, s, index, isSelected, cellHasFocus);
			}
		});
		comboBoxBigData.setEnabled(false);
		Settings.BIG_DATA = datasetLoader.loadBigDataEnabled();
		comboBoxBigData.setSelectedItem(Settings.BIG_DATA);
		comboBoxBigData.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				Settings.BIG_DATA = (Boolean) comboBoxBigData.getSelectedItem();
				updateDataset();
			}
		});

		builder.append("File:");
		builder.append(labelFile, 3);
		builder.nextLine();

		builder.append("Path:");
		builder.append(labelPath, 3);
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

		builder.append(Settings.text("dataset.big-data.disable-question"));
		builder.append(comboBoxBigData, 1);
		builder.nextLine();
		setLayout(new BorderLayout());
		add(builder.getPanel());

		// auto load last selected dataset
		if (oldDatasets.size() > 0)
		{
			//			if (oldDatasets.get(0).isLocal())

			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					load(oldDatasets.get(0).getPath()); // auto-load local files
				}
			});
			//			else
			//				textField.setText(oldDatasets.get(0).getPath()); //remote file, do not auto-load
		}
	}

	private void doImport()
	{
		JFileChooser chooser = null;
		String dir = PropHandler.get("workflow-import-dir");
		if (dir == null)
			dir = PropHandler.get("workflow-export-dir");
		if (dir == null)
			dir = System.getProperty("user.home");
		chooser = new JFileChooser(new File(dir));
		JPanel p = new JPanel(new BorderLayout());
		MessageLabel m = new MessageLabel(Message.infoMessage(
				"Imports settings for all wizard steps. Settings can be exported in the viewer.",
				MessageUtil.createURLAction(Settings.HOMEPAGE_EXPORT_SETTINGS)));
		m.setBorder(new EmptyBorder(5, 5, 5, 5));
		p.add(m, BorderLayout.NORTH);
		chooser.setAccessory(p);
		chooser.setFileFilter(new FileFilter()
		{
			@Override
			public String getDescription()
			{
				return "CheS-Mapper Wizard Settings File (*.ches)";
			}

			@Override
			public boolean accept(File f)
			{
				return f.isDirectory() || FileUtil.getFilenamExtension(f.getAbsolutePath()).matches("(?i)ches");
			}
		});
		chooser.showOpenDialog(this);
		final File f = chooser.getSelectedFile();
		if (f != null && FileUtil.getFilenamExtension(f.getAbsolutePath()).matches("(?i)ches"))
		{
			importFile(f);
		}
	}

	public void importFile(File f)
	{
		String p = f.getAbsolutePath();
		if (oldWorkflows.contains(p))
			oldWorkflows.remove(p);
		oldWorkflows.add(0, p);
		while (oldWorkflows.size() > 12)
			oldWorkflows.remove(12);
		PropHandler.put("workflow-list", ListUtil.toCSVString(oldWorkflows));
		PropHandler.put("workflow-import-dir", f.getParent());
		PropHandler.storeProperties();
		((CheSMapperWizard) DatasetWizardPanel.this.wizard).initImport(f);
	}

	private void updateDataset()
	{
		if (dataset == null)
		{
			labelFile.setText("-");
			labelPath.setText("-");
			labelProps.setText("-");
			labelSize.setText("-");
			label3D.setText("-");
			comboBoxBigData.setEnabled(false);
		}
		else
		{
			labelFile.setText(dataset.getName());
			labelPath.setText(dataset.getPath());
			//			labelFile.setText(dataset.getFullName());
			//			labelPath.setText(dataset.getLocalPath());
			labelProps.setText(dataset.getIntegratedProperties().length + "");
			labelSize.setText(dataset.numCompounds() + "");
			label3D.setText(dataset.has3D() + "");
			comboBoxBigData.setEnabled(true);
		}
		wizard.update();
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

	public void load(final String f)
	{
		if (wizard.isBlocked())
			throw new IllegalStateException();
		SwingUtil.checkIsAWTEventThread();
		if (f == null)
			throw new IllegalArgumentException();

		wizard.block(f);
		labelFile.setText("-");
		labelPath.setText("-");
		labelProps.setText("-");
		labelNumericProps.setText("-");
		labelSize.setText("-");
		label3D.setText("-");

		Thread th = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					dataset = datasetLoader.load(f);
					if (dataset != null && !dataset.isLoaded())
						throw new IllegalStateException();
				}
				finally
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							if (dataset != null)
								updateDataset();
							wizard.unblock(f);
						}
					});
				}
			}
		});
		th.start();
	}

	@Override
	public void proceed()
	{
		if (oldDatasets.contains(dataset))
			oldDatasets.removeElement(dataset);
		oldDatasets.add(0, dataset);
		while (oldDatasets.size() > 12)
			oldDatasets.remove(12);
		datasetLoader.store(oldDatasets, (Boolean) comboBoxBigData.getSelectedItem());
	}

	@Override
	public Messages canProceed()
	{
		if (getDatasetFile() != null)
		{
			boolean datasetIsBig = getDatasetFile().numCompounds() >= 1000;
			boolean bigDataModeEnabled = (Boolean) comboBoxBigData.getSelectedItem();
			if (datasetIsBig && !bigDataModeEnabled)
				return Messages.slowMessage(Settings.text("dataset.big-data.not-enabled-warning"));
			else if (bigDataModeEnabled)
			{
				String msg = Settings.text("dataset.big-data.enabled-warning");
				return datasetIsBig ? Messages.infoMessage(msg) : Messages.warningMessage(msg);
			}
			else
				return null;
		}
		else
			return Messages.errorMessage(""); // error is obvious (select dataset!)
	}

	public DatasetFile getDatasetFile()
	{
		if (dataset != null && !dataset.isLoaded())
			throw new IllegalStateException();
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

}
