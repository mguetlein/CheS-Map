package data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;

import main.Settings;

public abstract class RScriptUser
{
	protected abstract String getRScriptName();

	protected abstract String getRScriptCode();

	protected String getScriptPath()
	{

		String scriptPath = null;
		try
		{
			String path = Settings.BASE_DIR + File.separator + getRScriptName() + ".R";
			File f = new File(path);
			BufferedReader br = new BufferedReader(new StringReader(getRScriptCode()));
			BufferedWriter wr = new BufferedWriter(new FileWriter(f));
			String s = "";
			while ((s = br.readLine()) != null)
				wr.write(s + "\n");
			wr.flush();
			wr.close();
			br.close();
			scriptPath = path;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		if (!new File(scriptPath).exists())
			System.err.println("Rscript could not be created: " + scriptPath);
		else
			System.out.println("Rscript created: " + scriptPath);
		return scriptPath;
	}

	public static void main(String args[])
	{
		RScriptUser test = new RScriptUser()
		{
			@Override
			public String getRScriptName()
			{
				return "pca";
			}

			@Override
			protected String getRScriptCode()
			{
				return "args <- commandArgs(TRUE)\n" + "df = read.table(args[1])\n" + "res <- princomp(df)\n"
						+ "print(res$scores[,1:3])\n" + "write.table(res$scores[,1:3],args[2]) ";
			}

		};
		System.out.println(test.getScriptPath());
	}
}
