package org.chesmapper.map.data;

import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.chesmapper.map.dataInterface.CompoundPropertySet;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.mg.javalib.task.Task;
import org.mg.javalib.task.TaskDialog;
import org.mg.javalib.util.SequentialWorkerThread;

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

	public void loadFeature(CompoundPropertySet set, DatasetFile dataset, Window owner)
	{
		loadFeatures(new CompoundPropertySet[] { set }, dataset, owner);
	}

	public void loadFeatures(final CompoundPropertySet[] sets, final DatasetFile dataset, final Window owner)
	{
		worker.waitUntilDone();
		worker.addJob(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Task task = TaskProvider.initTask("Compute features");
					new TaskDialog(task, owner);
					int num = 0;
					for (CompoundPropertySet set : sets)
						if (!set.isComputed(dataset))
							num++;
					int step = 100 / num;
					int p = 0;
					for (CompoundPropertySet set : sets)
						if (!set.isComputed(dataset))
						{
							TaskProvider.update(p, " Compute feature: " + set);
							p += step;
							//ignoring boolean return values, if error occured, a warning should be given in compute
							set.compute(dataset);
							if (!TaskProvider.isRunning())
								break;
						}

					task.finish();
				}
				catch (Throwable e)
				{
					Settings.LOGGER.error(e);
					TaskProvider.failed("Could not compute features", e);
				}
				finally
				{
					TaskProvider.removeTask();
				}
				fireEvent("loaded");
			}
		}, "load features");
	}
}
