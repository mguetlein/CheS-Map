package main;

import task.Task;
import task.TaskImpl;

public class TaskProvider
{
	static TaskImpl currentTask;

	public synchronized static Task initTask(String name)
	{
		currentTask = new TaskImpl(name, Settings.LOGGER);
		return currentTask;
	}

	public static void removeTask()
	{
		currentTask = null;
	}

	public static void verbose(String verbose)
	{
		if (currentTask != null)
			currentTask.verbose(verbose);
	}

	public static void update(String update)
	{
		if (currentTask != null)
			currentTask.update(update);
	}

	public static boolean isRunning()
	{
		if (currentTask != null)
			return currentTask.isRunning();
		else
			return true;
	}

	public static void update(double i, String update)
	{
		if (currentTask != null)
			currentTask.update(i, update);
	}

	public static void warning(String warningMessage, Throwable exception)
	{
		if (currentTask != null)
			currentTask.warning(warningMessage, exception);
	}

	public static void warning(String warningMessage, String details)
	{
		if (currentTask != null)
			currentTask.warning(warningMessage, details);
	}

	public static void failed(String errorMessage, Throwable exception)
	{
		if (currentTask != null)
			currentTask.failed(errorMessage, exception);
	}

	public static void failed(String errorMessage, String details)
	{
		if (currentTask != null)
			currentTask.failed(errorMessage, details);
	}

}
