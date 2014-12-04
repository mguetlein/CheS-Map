package org.chesmapper.map.workflow;

import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.FeatureService.IllegalCompoundsException;
import org.chesmapper.map.main.PropHandler;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.chesmapper.map.opentox.DatasetUtil;
import org.mg.javalib.io.SDFUtil;
import org.mg.javalib.task.Task;
import org.mg.javalib.task.TaskDialog;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.ListUtil;
import org.mg.javalib.util.StringUtil;
import org.mg.javalib.util.SwingUtil;
import org.mg.javalib.util.ThreadUtil;
import org.mg.javalib.util.VectorUtil;

public class DatasetLoader implements DatasetMappingWorkflowProvider
{
	boolean showLoadDialog = false;

	private String propKeyDataset = "dataset-recently-used";
	private String propKeyBigData = "big-data-mode";

	public DatasetLoader(boolean showLoadDialog)
	{
		this.showLoadDialog = showLoadDialog;
		if (showLoadDialog && Settings.TOP_LEVEL_FRAME == null)
			Settings.LOGGER.error("load dialog only shows alongside wizard or viewer");
	}

	public Vector<DatasetFile> loadRecentlyLoadedDatasets()
	{
		String dir = PropHandler.get(propKeyDataset);
		Vector<DatasetFile> oldDatasets = new Vector<DatasetFile>();
		if (dir != null)
		{
			List<String> strings = StringUtil.split(dir);
			for (String s : strings)
				if (s != null && s.trim().length() > 0)
					oldDatasets.add(DatasetFile.fromString(s));
		}
		return oldDatasets;
	}

	public boolean loadBigDataEnabled()
	{
		String selected = PropHandler.get(propKeyBigData);
		return (selected != null && selected.equals("true"));
	}

	public void store(Vector<DatasetFile> oldDatasets, boolean bigDataEnabled)
	{
		Vector<String> strings = new Vector<String>();
		for (DatasetFile d : oldDatasets)
			strings.add(d.toString());
		PropHandler.put(propKeyDataset, VectorUtil.toCSVString(strings));
		PropHandler.put(propKeyBigData, bigDataEnabled + "");
		PropHandler.storeProperties();
	}

	@Override
	public DatasetFile exportDatasetToMappingWorkflow(String datasetPath, boolean bigDataMode, Properties props)
	{
		DatasetFile dataset = load(datasetPath);
		if (dataset != null)
		{
			props.put(propKeyDataset, dataset.toString());
			props.put(propKeyBigData, String.valueOf(bigDataMode));
		}
		return dataset;
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
		Settings.BIG_DATA = false;
		String selected = (String) props.get(propKeyBigData);
		if (selected != null && selected.equals("true"))
			Settings.BIG_DATA = true;
		if (storeToSettings)
			PropHandler.put(propKeyBigData, ((Boolean) Settings.BIG_DATA).toString());

		return load(df.getPath());
	}

	public DatasetFile load(String f)
	{
		SwingUtil.checkNoAWTEventThread();

		final DatasetFile d;
		boolean http = f.startsWith("http");
		if (http)
			d = DatasetFile.getURLDataset(f);
		else
			d = DatasetFile.localFile(f);
		if (d.isLoaded())
			return d;

		final Task task = TaskProvider.initTask("Loading dataset file");
		if (showLoadDialog)
		{
			if (Settings.TOP_LEVEL_FRAME != null && Settings.TOP_LEVEL_FRAME.isVisible())
				new TaskDialog(task, Settings.TOP_LEVEL_FRAME);
			else
			{
				Thread th = new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						while (Settings.TOP_LEVEL_FRAME == null || !Settings.TOP_LEVEL_FRAME.isVisible())
							ThreadUtil.sleep(100);
						if (task.isRunning())
							new TaskDialog(task, Settings.TOP_LEVEL_FRAME);
					}
				});
				th.start();
			}
		}

		TaskProvider.update("Load dataset: " + d.getName());
		try
		{
			if (!d.isLocal())
			{
				TaskProvider.debug("Downloading external dataset: " + d.getName());
				DatasetUtil.downloadDataset(d.getURI());
			}
			if (!TaskProvider.isRunning())
				return null;
			File datasetFile = new File(d.getLocalPath());
			if (datasetFile == null || !datasetFile.exists())
				throw new Exception("file not found: " + datasetFile.getAbsolutePath());
			d.loadDataset();
			if (!TaskProvider.isRunning())
				return null;
			if (d.numCompounds() == 0)
				throw new Exception("No compounds in file");
			task.finish();
			TaskProvider.removeTask();
			return d;
		}
		catch (final IllegalCompoundsException e)
		{
			// illegal compounds can only be handled in sdf or csv
			if (d.getFileExtension() == null || !d.getFileExtension().matches("(?i)(csv|sdf)"))
				throw new Error(e);
			Settings.LOGGER.error(e);
			task.cancel();
			TaskProvider.removeTask();
			return loadFiltered(d, e.illegalCompounds);
		}
		catch (Throwable e)
		{
			Settings.LOGGER.error(e);
			TaskProvider
					.failed("Could not load dataset: " + d.getPath(),
							"<html>"
									+ e.getClass().getSimpleName()
									+ ": '"
									+ e.getMessage()
									+ "'"
									+ (d.isLocal() ? ""
											: "<br>(Make sure to start the dataset URL with 'http' if you want to load an external dataset.)")
									+ "</html>");
			TaskProvider.removeTask();
			return null;
		}
	}

	private DatasetFile loadFiltered(final DatasetFile d, final List<Integer> illegalCompounds)
	{
		final StringBuffer cleanedFile = new StringBuffer();
		SwingUtil.invokeAndWait(new Runnable()
		{
			public void run()
			{
				int res = JOptionPane.showConfirmDialog(
						Settings.TOP_LEVEL_FRAME,
						"Could not read "
								+ illegalCompounds.size()
								+ " compound/s in dataset: "
								+ d.getPath()
								+ "\nIndices of compounds that could not be loaded: "
								+ ListUtil.toString(illegalCompounds)
								+ "\n\nDo you want to skip the faulty compounds, store the correct compounds in a new file, and reload this new file?",
						"Dataset faulty", JOptionPane.YES_NO_OPTION);
				if (res != JOptionPane.YES_OPTION)
					return;
				String parent = FileUtil.getParent(d.getLocalPath());
				String file = parent + File.separator + d.getShortName() + "_cleaned." + d.getFileExtension();
				JFileChooser fc = new JFileChooser(parent);
				fc.setSelectedFile(new File(file));
				int res2 = fc.showSaveDialog(Settings.TOP_LEVEL_FRAME);
				if (res2 != JFileChooser.APPROVE_OPTION)
					return;
				if (fc.getSelectedFile().exists())
				{
					int res3 = JOptionPane.showConfirmDialog(Settings.TOP_LEVEL_FRAME, "File '"
							+ fc.getSelectedFile().getAbsolutePath() + "' already exists. Overwrite?",
							"Overwrite existing file?", JOptionPane.YES_NO_OPTION);
					if (res3 != JOptionPane.YES_OPTION)
						return;
				}
				cleanedFile.append(fc.getSelectedFile().getAbsolutePath());
			}
		});
		String cleanedFileStr = cleanedFile.toString();
		if (cleanedFileStr.length() == 0)
			return null;
		if (d.getFileExtension().matches("(?i)sdf"))
			SDFUtil.filter_exclude(d.getSDF(), cleanedFileStr, illegalCompounds, false);
		else if (d.getFileExtension().matches("(?i)csv"))
		{
			String all = FileUtil.readStringFromFile(d.getLocalPath());
			StringBuffer cleaned = new StringBuffer();
			for (String line : all.split("\n"))
			{
				{
					cleaned.append(line);
					cleaned.append("\n");
				}
			}
			FileUtil.writeStringToFile(cleanedFileStr, cleaned.toString());
			ThreadUtil.sleep(1000);
		}
		return load(cleanedFileStr);
	}
}
