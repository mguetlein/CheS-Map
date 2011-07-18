package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
import util.FileUtil;
import util.SwingUtil;
import util.VectorUtil;
import alg.DatasetProvider;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

import data.CDKService;

public class DatasetWizardPanel extends WizardPanel implements DatasetProvider
{
	JTextField textField;
	JFileChooser chooser;
	JList recentlyUsed;

	JButton buttonLoad;

	JLabel labelFile;
	JLabel labelSize;
	JLabel labelProps;
	JLabel label3D;

	WizardDialog wizard;

	boolean loading = false;

	private static class Dataset
	{
		boolean isURL;
		String name;
		String url;
		String localPath;

		private Dataset(boolean isURL, String name, String url, String localPath)
		{
			this.isURL = isURL;
			this.name = name;
			this.url = url;
			this.localPath = localPath;
		}

		public static Dataset getURLDataset(String url)
		{
			return new Dataset(true, url, url, Settings.destinationFileForURL(url));
		}

		public static Dataset getLocalDataset(String path)
		{
			return new Dataset(false, FileUtil.getFilename(path), null, path);
		}

		public static Dataset fromString(String s)
		{
			String ss[] = s.split("#");
			return new Dataset(Boolean.parseBoolean(ss[0]), ss[1], ss[2], ss[3]);
		}

		/**
		 * textfield
		 * 
		 * @return
		 */
		public String getPath()
		{
			if (isURL)
				return url;
			else
				return localPath;
		}

		public String toString()
		{
			return isURL + "#" + name + "#" + url + "#" + localPath;
		}

		public boolean equals(Object o)
		{
			if (o instanceof Dataset)
				return toString().equals(((Dataset) o).toString());
			else
				return false;
		}
	}

	Vector<Dataset> oldDatasets;

	private Dataset dataset;

	public DatasetWizardPanel(WizardDialog wizard)
	{
		this.wizard = wizard;
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(
				"pref,5dlu,pref:grow(0.99),5dlu,right:p:grow(0.01)"));

		builder.append("Select sdf file:");
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
		oldDatasets = new Vector<Dataset>();
		if (dir != null)
		{
			Vector<String> strings = VectorUtil.fromCSVString(dir);
			for (String s : strings)
				oldDatasets.add(Dataset.fromString(s));
		}
		final DefaultListModel m = new DefaultListModel();
		for (Dataset d : oldDatasets)
			m.addElement(d);
		recentlyUsed = new JList(m);
		recentlyUsed.setFont(recentlyUsed.getFont().deriveFont(Font.PLAIN));
		recentlyUsed.setCellRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				Dataset d = (Dataset) value;
				String s = "<html><b>" + d.name + "</b><br>(" + d.localPath + ")</html>";
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
					load((Dataset) recentlyUsed.getSelectedValue());
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

		buttonLoad = new JButton("Load dataset");
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
			load(oldDatasets.get(0));
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
			int num = CDKService.loadSdf(dataset.localPath);
			labelFile.setText(dataset.name);
			labelProps.setText(CDKService.getSDFProperties(dataset.localPath).length + "");
			labelSize.setText(num + "");
			label3D.setText(CDKService.has3D(dataset.localPath) + "");
			if (oldDatasets.indexOf(dataset) == -1)
				recentlyUsed.setSelectedValue(dataset, true);
		}
		wizard.update();
	}

	private String loadHttpFile(String datasetUrl)
	{
		try
		{
			URL url = new URL(datasetUrl);
			File f = new File(Settings.destinationFileForURL(datasetUrl));
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("GET");
			connection.setRequestProperty("accept", "chemical/x-mdl-sdf");
			BufferedWriter buffy = new BufferedWriter(new FileWriter(f));
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String s = "";
			while ((s = reader.readLine()) != null)
				buffy.write(s + "\n");
			buffy.flush();
			buffy.close();
			reader.close();
			connection.disconnect();
			if (connection.getResponseCode() >= 400)
				throw new Exception("Response code: " + connection.getResponseCode());
			return f.getAbsolutePath();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(DatasetWizardPanel.this.getTopLevelAncestor(),
					"Error: could not load dataset from: '" + datasetUrl + "'\nError type: '"
							+ e.getClass().getSimpleName() + "'\nMessage: '" + e.getMessage() + "'",
					"Http Connection Error", JOptionPane.ERROR_MESSAGE);
		}
		return null;
	}

	//	public static void main(String args[])
	//	{
	//		System.out.println(loadHttpFile("http://apps.ideaconsult.net:8080/ambit2/dataset/272?max=5"));
	//	}

	private void load(Dataset d)
	{
		if (d.isURL)
			load(d.url);
		else
			load(d.localPath);
	}

	public void load(String f)
	{
		if (loading)
			return;
		loading = true;

		dataset = null;
		if (!textField.getText().equals(f))
			textField.setText(f);
		buttonLoad.setEnabled(false);

		final Dataset d;
		boolean http = f.startsWith("http");
		if (http)
			d = Dataset.getURLDataset(f);
		else
			d = Dataset.getLocalDataset(f);

		if (CDKService.isLoaded(f))
		{
			if (CDKService.loadSdf(f) > 0)
				dataset = d;
			updateDataset();
			loading = false;
		}
		else
		{
			SwingUtil.loadingLabel(labelFile);
			labelProps.setText("-");
			labelSize.setText("-");
			label3D.setText("-");

			Thread th = new Thread(new Runnable()
			{
				public void run()
				{
					if (d.isURL)
						loadHttpFile(d.url);
					File datasetFile = new File(d.localPath);
					if (datasetFile != null && datasetFile.exists() && CDKService.loadSdf(d.localPath) > 0)
						dataset = d;
					else
					{
						if (!d.isURL && d.localPath.length() > 0)
							JOptionPane.showMessageDialog(
									DatasetWizardPanel.this.getTopLevelAncestor(),
									"Local dataset not found:\n"
											+ d.getPath()
											+ "\n\n(Make sure to start the dataset URL with 'http' if you want to load an external dataset.)",
									"Dataset not found", JOptionPane.ERROR_MESSAGE);
						buttonLoad.setEnabled(true);
					}
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
		for (Dataset d : oldDatasets)
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
		return "Load dataset";
	}

	@Override
	public String getDatasetFile()
	{
		if (dataset != null)
			return dataset.localPath;
		else
			return null;
	}

	@Override
	public String getDatasetName()
	{
		if (dataset != null)
			return dataset.name;
		else
			return null;
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
