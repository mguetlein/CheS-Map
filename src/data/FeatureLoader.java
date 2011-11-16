package data;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import main.Settings;
import main.TaskProvider;
import util.SequentialWorkerThread;
import util.SwingUtil;
import dataInterface.MoleculePropertySet;

public class FeatureLoader
{
	public static FeatureLoader instance = new FeatureLoader();
	private List<PropertyChangeListener> listeners = new ArrayList<PropertyChangeListener>();
	private SequentialWorkerThread worker = new SequentialWorkerThread();

	private FeatureLoader()
	{
	}

	public void addPropertyChangeListener(PropertyChangeListener l)
	{
		listeners.add(l);
	}

	public void removePropertyChangeListener(PropertyChangeListener l)
	{
		listeners.remove(l);
	}

	private void fireEvent(String prop)
	{
		for (PropertyChangeListener ll : listeners)
			ll.propertyChange(new PropertyChangeEvent(this, prop, false, true));
	}

	public void loadFeature(MoleculePropertySet set, DatasetFile dataset)
	{
		loadFeatures(new MoleculePropertySet[] { set }, dataset);
	}

	public void loadFeatures(final MoleculePropertySet[] sets, final DatasetFile dataset)
	{
		worker.waitUntilDone();
		worker.addJob(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					TaskProvider.registerThread("Compute features");
					TaskProvider.task().showDialog((JFrame) Settings.TOP_LEVEL_COMPONENT, "Computing features");
					int num = 0;
					for (MoleculePropertySet set : sets)
						if (!set.isComputed(dataset))
							num++;
					int step = 100 / num;
					int p = 0;
					for (MoleculePropertySet set : sets)
						if (!set.isComputed(dataset))
						{
							TaskProvider.task().update(p, " Compute feature: " + set);
							p += step;
							set.compute(dataset);
							if (TaskProvider.task().isCancelled())
								break;
						}
					TaskProvider.task().getDialog().setVisible(false);
				}
				catch (Throwable e)
				{
					e.printStackTrace();
					TaskProvider.task().error(e.getMessage(), e);
					if (TaskProvider.task().getDialog() != null)
						SwingUtil.waitWhileVisible(TaskProvider.task().getDialog());
				}
				finally
				{
					TaskProvider.clear();
				}
				fireEvent("loaded");
			}
		}, "load features");
	}
}
