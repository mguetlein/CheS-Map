package rscript;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;

import main.Settings;
import util.FileUtil;

public class RScriptUtil
{

	private static String REPOSITORY = "http://cran.r-project.org";

	// private static String REPOSITORY = "http://ftp5.gwdg.de/pub/misc/cran"; // göttingen

	public static String installAndLoadPackage(String pack)
	{
		return ".libPaths(\"" + FileUtil.getAbsolutePathEscaped(new File(Settings.R_LIB_DIR))
				+ "\")\n"
				+ //
				"packages <- installed.packages()[,1]\n" //
				+ "if (!(is.element(\"" + pack + "\", packages))) install.packages(\"" + pack + "\",repos=\""
				+ REPOSITORY + "\",lib=\"" + FileUtil.getAbsolutePathEscaped(new File(Settings.R_LIB_DIR)) + "\")\n" + //
				"library(\"" + pack + "\")\n";
	}

	public static String getScriptPath(String scriptName, String scriptCode)
	{
		String scriptPath = null;
		try
		{
			String path = Settings.BASE_DIR + File.separator + scriptName + ".R";
			File f = new File(path);
			BufferedReader br = new BufferedReader(new StringReader(scriptCode));
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
			Settings.LOGGER.error(e);
		}
		if (!new File(scriptPath).exists())
			Settings.LOGGER.warn("Rscript could not be created: " + scriptPath);
		else
			Settings.LOGGER.info("Rscript created: " + scriptPath);
		return scriptPath;
	}

}
