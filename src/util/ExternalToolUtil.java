package util;

import io.ExternalTool;

import java.io.File;

import main.TaskProvider;

public class ExternalToolUtil
{
	public static void run(String processName, String cmd)
	{
		run(processName, cmd, null);
	}

	public static void run(String processName, String cmd, File stdOutFile)
	{
		ExternalTool ext = new ExternalTool()
		{
			protected void stdout(String s)
			{
				if (!TaskProvider.exists())
					TaskProvider.registerThread("Ches-Mapper-Task");
				TaskProvider.task().verbose(s);
				System.out.println(s);
			}

			protected void stderr(String s)
			{
				if (!TaskProvider.exists())
					TaskProvider.registerThread("Ches-Mapper-Task");
				TaskProvider.task().verbose(s);
				System.err.println(s);
			}
		};
		Process p = ext.run(processName, cmd, stdOutFile, stdOutFile != null);
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
			if (TaskProvider.task().isCancelled())
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
	}
}
