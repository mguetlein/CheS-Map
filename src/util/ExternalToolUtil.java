package util;

import io.ExternalTool;

import java.io.File;

import main.TaskProvider;

public class ExternalToolUtil
{
	public static String run(String processName, String cmd[])
	{
		return run(processName, cmd, null);
	}

	public static String run(String processName, String cmd)
	{
		return run(processName, cmd, null);
	}

	public static String run(String processName, String cmd[], File stdOutFile)
	{
		return run(processName, cmd, stdOutFile, null);
	}

	public static String run(String processName, String cmd, File stdOutFile)
	{
		return run(processName, cmd, stdOutFile, null);
	}

	public static String run(String processName, Object cmdStringOrArray, File stdOutFile, String env[])
	{
		ExternalTool ext = new ExternalTool()
		{
			protected void stdout(String s)
			{
				TaskProvider.verbose(s);
				System.out.println(s);
			}

			protected void stderr(String s)
			{
				TaskProvider.verbose(s);
				System.err.println(s);
			}
		};
		Process p;
		if (cmdStringOrArray instanceof String)
			p = ext.run(processName, (String) cmdStringOrArray, stdOutFile, stdOutFile != null, env);
		else
			p = ext.run(processName, (String[]) cmdStringOrArray, stdOutFile, stdOutFile != null, env);
		while (true)
		{
			try
			{
				Thread.sleep(200);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
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
				p.exitValue();
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
