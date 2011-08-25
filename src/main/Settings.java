package main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import util.FileUtil;
import util.ImageLoader;
import util.OSUtil;
import weka.core.Version;
import data.CDKSmartsHandler;
import dataInterface.SmartsHandler;

public class Settings
{
	// -------------------------- GRAPHICAL STUFF ---------------------------------

	static
	{
		try
		{
			Font font = new Font("Dialog", Font.PLAIN, 12);
			UIDefaults uiDefaults = UIManager.getDefaults();
			String comps[] = { "Label", "CheckBox", "List", "RadioButton", "Table" };
			for (String s : comps)
				uiDefaults.put(s + ".font", font);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static Color BACKGROUND = Color.BLACK;
	public static Color FOREGROUND = new Color(170, 170, 170);
	public static Color LIST_SELECTION_FOREGROUND = FOREGROUND.brighter().brighter();
	public static Color LIST_ACTIVE_BACKGROUND = new Color(51, 102, 255);

	//	public static Color BACKGROUND = Color.WHITE;
	//	public static Color FOREGROUND = new Color(50, 50, 50);
	//	public static Color LIST_SELECTION_FOREGROUND = FOREGROUND.darker().darker();
	//	public static Color LIST_ACTIVE_BACKGROUND = new Color(101, 152, 255);

	public static Color TRANSPARENT_BACKGROUND = new Color(BACKGROUND.getRed(), BACKGROUND.getGreen(),
			BACKGROUND.getBlue(), 200);
	public static Color LIST_WATCH_BACKGROUND = new Color(LIST_ACTIVE_BACKGROUND.getRed(),
			LIST_ACTIVE_BACKGROUND.getGreen(), LIST_ACTIVE_BACKGROUND.getBlue(), 100);

	public static Component TOP_LEVEL_COMPONENT = null;
	public static Random RANDOM = new Random();
	public static Boolean DBG = false;
	public static final ImageIcon CHES_MAPPER_IMAGE = ImageLoader.CHES_MAPPER;
	public static final ImageIcon CHES_MAPPER_IMAGE_SMALL = ImageLoader.CHES_MAPPER_SMALL;
	public static final ImageIcon OPENTOX_ICON = ImageLoader.OPENTOX;

	// ------------------------ EXTERNAL PROGRAMMS -----------------------

	public static String CM_BABEL_PATH = null;
	public static String CM_OBFIT_PATH = null;
	public static String CM_RSCRIPT_PATH = null;

	public static final String BABEL_NOT_FOUND_WARNING = "OpenBabel command 'babel' could not be found.";

	// TODO ------------ fix spaghetti code ------------------------------
	static
	{
		System.err.println("\nfinding binaries - start");

		System.err.println("* try env variables");
		Map<String, String> env = System.getenv();
		if (env.get("CM_BABEL_PATH") != null)
		{
			CM_BABEL_PATH = env.get("CM_BABEL_PATH");
			System.err.println("babel-path set to: " + CM_BABEL_PATH);
		}
		if (env.get("CM_OBFIT_PATH") != null)
		{
			CM_OBFIT_PATH = env.get("CM_OBFIT_PATH");
			System.err.println("obfit-path set to: " + CM_OBFIT_PATH);
		}
		if (env.get("CM_RSCRIPT_PATH") != null)
		{
			CM_RSCRIPT_PATH = env.get("CM_RSCRIPT_PATH");
			System.err.println("Rscript-path set to: " + CM_RSCRIPT_PATH);
		}

		System.err.println("* try locally (binary has to be available in PATH)");
		if (CM_BABEL_PATH == null && exitValue("babel -H") == 0)
		{
			CM_BABEL_PATH = "babel";
			System.err.println("babel found locally or in PATH");
		}
		if (CM_OBFIT_PATH == null && exitValue("obfit") == 255)
		{
			CM_OBFIT_PATH = "obfit";
			System.err.println("obfit found locally or in PATH");
		}
		if (CM_RSCRIPT_PATH == null && exitValue("Rscript --help") == 0)
		{
			CM_RSCRIPT_PATH = "Rscript";
			System.err.println("Rscript found locally or in PATH");
		}

		if (OSUtil.isUnix() || OSUtil.isMac())
		{
			System.err.println("* try to find babel/obfit in /usr/local/bin");
			if (CM_BABEL_PATH == null && exitValue("/usr/local/bin/babel -H") == 0)
			{
				CM_BABEL_PATH = "/usr/local/bin/babel";
				System.err.println("babel found in /usr/local/bin/");
			}
			if (CM_OBFIT_PATH == null && exitValue("/usr/local/bin/obfit") == 255)
			{
				CM_OBFIT_PATH = "/usr/local/bin/obfit";
				System.err.println("obfit found in /usr/local/bin/");
			}

			System.err.println("* try to find with 'which'");
			if (CM_BABEL_PATH == null)
			{
				CM_BABEL_PATH = findExecutableLinux("babel");
				if (CM_BABEL_PATH != null)
					System.err.println("babel-path found at: " + CM_BABEL_PATH);
			}
			if (CM_OBFIT_PATH == null)
			{
				CM_OBFIT_PATH = findExecutableLinux("obfit");
				if (CM_OBFIT_PATH != null)
					System.err.println("obfit found at: " + CM_OBFIT_PATH);
			}
			if (CM_RSCRIPT_PATH == null)
			{
				CM_RSCRIPT_PATH = findExecutableLinux("Rscript");
				if (CM_RSCRIPT_PATH != null)
					System.err.println("Rscript found at: " + CM_RSCRIPT_PATH);
			}
		}
		else if (OSUtil.isWindows())
		{
			String openbabelDir = findWinOpenBabelDir();
			if (openbabelDir != null)
			{
				System.err.println("* try to find babel/obfit in " + openbabelDir);
				if (CM_BABEL_PATH == null && new File(openbabelDir + "\\babel.exe").exists())
				{
					CM_BABEL_PATH = openbabelDir + "\\babel";
					System.err.println("babel found in " + openbabelDir);
				}
				if (CM_OBFIT_PATH == null && new File(openbabelDir + "\\obfit.exe").exists())
				{
					CM_OBFIT_PATH = openbabelDir + "\\obfit";
					System.err.println("obfit found in " + openbabelDir);
				}
			}
		}

		System.err.println("finding binaries - end\n");
	}

	private static String findExecutableLinux(String executable)
	{
		try
		{
			String command = "which " + executable;
			Status.WARN.println("try to find " + executable + " with '" + command + "'");
			Process child = Runtime.getRuntime().exec(command);
			child.waitFor();
			if (child.exitValue() == 1)
				throw new Exception("failed: " + command);
			String res = new BufferedReader(new InputStreamReader(child.getInputStream())).readLine().trim();
			if (res.length() == 0)
				throw new Exception("return value of command '" + command + "' empty");
			else
				return res;
		}
		catch (Exception e)
		{
			Status.WARN.println("Could not find executable '" + executable + "' :\n" + e.getMessage());
			return null;
		}
	}

	private static int exitValue(String command)
	{
		try
		{
			Status.WARN.println("trying to run '" + command + "'");
			Process child = Runtime.getRuntime().exec(command);
			BufferedReader reader = new BufferedReader(new InputStreamReader(child.getInputStream()));
			while (reader.readLine() != null)
			{
			}
			BufferedReader reader2 = new BufferedReader(new InputStreamReader(child.getErrorStream()));
			while (reader2.readLine() != null)
			{
			}
			reader.close();
			reader2.close();
			child.waitFor();
			return child.exitValue();
		}
		catch (Exception e)
		{
			return -1;
		}
	}

	private static String findWinOpenBabelDir()
	{
		try
		{
			File d = new File("C:\\program files");
			if (d.isDirectory())
			{
				String dirs[] = d.list(new FilenameFilter()
				{
					@Override
					public boolean accept(File dir, String name)
					{
						return name.contains("OpenBabel");
					}
				});
				if (dirs != null && dirs.length > 0)
				{
					return "C:\\program files\\" + dirs[0];
				}
			}
		}
		catch (Exception e)
		{
		}
		return null;
	}

	// ------------------ TMP/RESULT-FILE SUPPORT ---------------------------------------------

	public static String BUILD_DATE = null;
	static
	{
		//ADD TO BUILD SCRIPT:
		//		   <tstamp>
		//		     <format property="TODAY" pattern="yyyy-MM-dd HH:mm" />
		//		   </tstamp>
		//            <manifest>
		//            	 <attribute name="Built-Date" value="${TODAY}"/>  
		//            </manifest>
		try
		{
			URL manifestUrl = Settings.class.getResource("/META-INF/MANIFEST.MF");
			//			System.out.println(manifestUrl);
			BufferedReader br = new BufferedReader(new InputStreamReader(manifestUrl.openStream()));
			String line;
			while ((line = br.readLine()) != null)
			{
				//				System.out.println(line);
				if (line.matches("(?i).*built.*date.*"))
				{
					Settings.BUILD_DATE = line;
					break;
				}
			}
			//			System.out.println(BUILD_DATE);
		}
		catch (Exception e)
		{
			//			e.printStackTrace();
		}
	}

	public static String VERSION = "v?.?.?";
	static
	{
		try
		{
			URL u = Settings.class.getResource("/VERSION");
			BufferedReader r;
			if (u == null)
				r = new BufferedReader(new FileReader(new File("VERSION")));
			else
				r = new BufferedReader(new InputStreamReader(u.openStream()));
			VERSION = r.readLine();
			r.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static String VERSION_STRING = VERSION + " Initial Prototype"
			+ ((BUILD_DATE != null) ? (", " + BUILD_DATE) : "");
	public static String TITLE = "CheS-Mapper";
	public static String HOMEPAGE = "http://opentox.informatik.uni-freiburg.de/ches-mapper";
	public static String CDK_VERSION = "1.4.0";
	public static String CDK_STRING = "The Chemistry Development Kit (CDK) (Version " + CDK_VERSION
			+ ", see http://cdk.sourceforge.net)";
	public static String OPENBABEL_STRING = "Open Babel: The Open Source Chemistry Toolbox (http://openbabel.org)";
	public static String WEKA_STRING = "WEKA : Data Mining Software in Java (Version " + Version.VERSION
			+ ", see http://www.cs.waikato.ac.nz/ml/weka)";
	public static String R_STRING = "The R Project for Statistical Computing (http://www.r-project.org)";
	public static String SMSD_STRING = "The Small Molecule Subgraph Detector (SMSD) (see http://www.ebi.ac.uk/thornton-srv/software/SMSD, integrated into CDK)";

	// ------------------ TMP/RESULT-FILE SUPPORT --------------------------------------------- 

	public static String BASE_DIR = System.getProperty("user.home") + File.separator + ".ches-mapper";
	public static String STRUCTURAL_ALERTS_DIR = BASE_DIR + File.separator + "structural_alerts";

	public static SmartsHandler SMARTS_HANDLER = new CDKSmartsHandler();

	static
	{
		for (String d : new String[] { BASE_DIR, STRUCTURAL_ALERTS_DIR })
		{
			File dir = new File(d);
			if (!dir.exists())
				dir.mkdir();
			if (!dir.exists())
				throw new Error("Could not create '" + d + "'");
		}
		// extract alerts
	}

	public static String[] getAlertFiles()
	{
		String alerts[] = new File(STRUCTURAL_ALERTS_DIR).list(new FilenameFilter()
		{

			@Override
			public boolean accept(File dir, String name)
			{
				return name.endsWith(".csv");
			}
		});
		for (int i = 0; i < alerts.length; i++)
			alerts[i] = STRUCTURAL_ALERTS_DIR + File.separator + alerts[i];
		return alerts;
	}

	public static String destinationFileForURL(String url)
	{
		try
		{
			return BASE_DIR + File.separator + URLEncoder.encode(url, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public static String getAlertFileDestination(String string)
	{
		return STRUCTURAL_ALERTS_DIR + File.separator + string;
	}

	public static String destinationFile(String sourceFilePath, String destinationFilename)
	{
		try
		{
			if (destinationFilename.startsWith("http://"))
				destinationFilename = URLEncoder.encode(destinationFilename, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
			return null;
		}

		if (sourceFilePath.startsWith(BASE_DIR))
			return FileUtil.getParent(sourceFilePath) + File.separator + destinationFilename;
		else
		{
			String path = FileUtil.getParent(sourceFilePath);
			if (OSUtil.isWindows() && path.charAt(1) == ':')
				path = path.charAt(0) + path.substring(2);
			String parent = BASE_DIR + File.separator + path;
			File dir = new File(parent);
			if (!dir.exists())
				dir.mkdirs();
			if (!dir.exists())
				throw new Error("could not create: " + dir);
			return dir.getAbsolutePath() + File.separator + destinationFilename;
		}
	}

	// ------------- LOAD AND STORE PROPS ----------------------------------	

	public static Properties PROPS;
	public static final String PROPERTIES_FILE = BASE_DIR + File.separator + "ches.mapper." + VERSION + ".props";

	static
	{
		//try loading props
		PROPS = new Properties();
		try
		{
			FileInputStream in = new FileInputStream(PROPERTIES_FILE);
			PROPS.load(in);
			in.close();
			//			System.out.println("property-keys: " + CollectionUtil.toString(PROPS.keySet()));
			//			System.out.println("property-values: " + CollectionUtil.toString(PROPS.values()));
		}
		catch (Exception e)
		{
			Status.WARN.println("Could not load properties: " + PROPERTIES_FILE);
		}
	}

	public static void storeProps()
	{
		try
		{
			FileOutputStream out = new FileOutputStream(PROPERTIES_FILE);
			PROPS.store(out, "---No Comment---");
			out.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	// ------------- ABORT MAPPING FUNCTIONS ----------------------------------

	private static Vector<Thread> abortedThreads = new Vector<Thread>();

	public static void abortThread(Thread th)
	{
		abortedThreads.add(th);
	}

	public static boolean isAborted(Thread th)
	{
		return abortedThreads.contains(th);
	}

}
