package gui;

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
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import main.PropHandler;
import main.Settings;
import util.SwingUtil;
import workflow.DatasetLoader;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

import connect.ChEMBLSearch;
import data.DatasetFile;

public class DatasetWizardPanel extends WizardPanel
{
	JTextField textField;
	JFileChooser chooser;
	JList<DatasetFile> recentlyUsed;

	JButton buttonLoad;

	JLabel labelFile;
	JLabel labelSize;
	JLabel labelProps;
	JLabel labelNumericProps;
	JLabel label3D;
	JComboBox<Boolean> comboBoxBigData;

	WizardDialog wizard;
	Vector<DatasetFile> oldDatasets;

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
				if (DatasetWizardPanel.this.wizard.isBlocked())
					return;
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
				int ret = chooser.showOpenDialog(DatasetWizardPanel.this.wizard);
				if (ret == JFileChooser.APPROVE_OPTION)
				{
					File f = chooser.getSelectedFile();
					if (f != null)
					{
						PropHandler.put("dataset-current-dir", f.getParent());
						PropHandler.storeProperties();
						//textField.setText(f.getPath());
						load(f.getPath());
					}
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

		oldDatasets = datasetLoader.loadRecentlyLoadedDatasets();
		final DefaultListModel<DatasetFile> m = new DefaultListModel<DatasetFile>();
		for (DatasetFile d : oldDatasets)
			m.addElement(d);
		recentlyUsed = new JList<DatasetFile>(m);
		recentlyUsed.setFont(recentlyUsed.getFont().deriveFont(Font.PLAIN));
		recentlyUsed.setCellRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
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

	private void updateDataset()
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
		if (!f.equals(textField.getText()))
			textField.setText(f);
		labelFile.setText("-");
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
							{
								if (dataset.isLocal() && !dataset.getLocalPath().equals(textField.getText()))
									textField.setText(dataset.getLocalPath());
								updateDataset();
								buttonLoad.setEnabled(false);
							}
							else
								buttonLoad.setEnabled(textField.getText().length() > 0);
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
