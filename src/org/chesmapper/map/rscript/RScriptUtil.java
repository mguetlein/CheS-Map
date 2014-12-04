package org.chesmapper.map.rscript;

import java.io.File;

import org.chesmapper.map.main.Settings;
import org.mg.javalib.util.FileUtil;

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
}
