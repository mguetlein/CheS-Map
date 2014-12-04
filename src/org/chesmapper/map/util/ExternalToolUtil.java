package org.chesmapper.map.util;

import java.io.File;

import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.mg.javalib.io.ExternalTool;

public class ExternalToolUtil
{
	public static String run(String processName, String cmd[])
	{
		return run(processName, cmd, null);
	}

	public static String run(String processName, String cmd[], File stdOutFile)
	{
		return run(processName, cmd, stdOutFile, null);
	}

	public static String run(String processName, String cmd[], File stdOutFile, String env[])
	{
		TaskProvider.debug("Run " + processName);
		ExternalTool ext = new ExternalTool(Settings.LOGGER)
		{
			protected void stdout(String s)
			{
				TaskProvider.verbose(s);
				Settings.LOGGER.info(s);
			}

			protected void stderr(String s)
			{
				TaskProvider.verbose(s);
				Settings.LOGGER.warn(s);
			}
		};
		Process p = ext.run(processName, cmd, stdOutFile, stdOutFile != null, env);
		while (true)
		{
			try
			{
				Thread.sleep(200);
			}
			catch (InterruptedException e)
			{
				Settings.LOGGER.error(e);
			}
			// check if this process should be aborted (via abort dialog)
			if (!TaskProvider.isRunning())
			{
				p.destroy();
				break;
			}
			// hack to determine if process has finished
			try
			{
				Settings.LOGGER.debug("Exit value: " + p.exitValue());
				break;
			}
			catch (IllegalThreadStateException e)
			{
				// this exception is thrown if the process has not finished
			}
		}
		return ext.getErrorOut();
	}
}
