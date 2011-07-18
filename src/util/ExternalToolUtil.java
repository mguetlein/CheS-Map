package util;

import io.ExternalTool;
import main.Settings;

public class ExternalToolUtil
{
	public static void run(String processName, String cmd)
	{
		Process p = ExternalTool.run(processName, cmd, false);
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
			if (Settings.isAborted(Thread.currentThread()))
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
