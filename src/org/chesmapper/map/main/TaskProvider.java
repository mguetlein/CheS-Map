package org.chesmapper.map.main;

import org.mg.javalib.task.Task;
import org.mg.javalib.task.TaskImpl;

public class TaskProvider
{
	static TaskImpl currentTask;
	static boolean printVerbose = false;

	public synchronized static Task initTask(String name)
	{
		currentTask = new TaskImpl(name, Settings.LOGGER);
		return currentTask;
	}

	public static void removeTask()
	{
		currentTask = null;
	}

	/**
	 * will be logged
	 */
	public static void debug(String verbose)
	{
		if (currentTask != null)
			currentTask.debug(verbose);
		else
			Settings.LOGGER.debug(verbose);
	}

	/**
	 * will not be logged (by default)
	 */
	public static void verbose(String verbose)
	{
		if (printVerbose)
			debug(verbose);
		else if (currentTask != null)
			currentTask.verbose(verbose);
	}

	public static void setPrintVerbose(boolean b)
	{
		printVerbose = b;
	}

	public static void update(String update)
	{
		if (currentTask != null)
			currentTask.update(update);
		else
			Settings.LOGGER.info(update);
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
		else
			Settings.LOGGER.info(update);
	}

	public static void warning(String warningMessage, Throwable exception)
	{
		if (currentTask != null)
			currentTask.warning(warningMessage, exception);
		else
		{
			Settings.LOGGER.warn(warningMessage + " : ");
			Settings.LOGGER.error(exception);
		}
	}

	public static void warning(String warningMessage, String details)
	{
		if (currentTask != null)
			currentTask.warning(warningMessage, details);
		else
		{
			Settings.LOGGER.warn(warningMessage + " : ");
			Settings.LOGGER.warn(details);
		}
	}

	public static void failed(String errorMessage, Throwable exception)
	{
		if (currentTask != null)
			currentTask.failed(errorMessage, exception);
		else
		{
			Settings.LOGGER.error(errorMessage + " : ");
			Settings.LOGGER.error(exception);
		}

	}

	public static void failed(String errorMessage, String details)
	{
		if (currentTask != null)
			currentTask.failed(errorMessage, details);
		else
		{
			Settings.LOGGER.error(errorMessage + " : ");
			Settings.LOGGER.error(details);
		}
	}

}
