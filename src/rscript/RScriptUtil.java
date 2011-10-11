package rscript;

public class RScriptUtil
{

	private static String REPOSITORY = "http://cran.r-project.org";

	//private static String REPOSITORY = "http://ftp5.gwdg.de/pub/misc/cran"; // göttingen

	public static String installAndLoadPackage(String pack)
	{
		return "packages <- installed.packages()[,1]\n" // 
				+ "if (!(is.element(\"" + pack + "\", packages))) install.packages(\"" + pack
				+ "\",repos=\""
				+ REPOSITORY + "\",dependencies = TRUE)\n" + "library(\"" + pack + "\")\n";
	}
}
