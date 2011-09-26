package main;

import gui.LinkButton;
import gui.binloc.Binary;
import gui.binloc.BinaryLocator;
import gui.binloc.BinaryLocatorDialog;
import io.ExternalTool;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import util.FileUtil;
import util.ImageLoader;
import util.OSUtil;
import weka.core.Version;

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
	public static Color LIST_WATCH_BACKGROUND = LIST_ACTIVE_BACKGROUND.darker().darker();

	public static Component TOP_LEVEL_COMPONENT = null;
	public static Random RANDOM = new Random();
	public static Boolean DBG = false;
	public static final ImageIcon CHES_MAPPER_IMAGE = ImageLoader.CHES_MAPPER;
	public static final ImageIcon CHES_MAPPER_IMAGE_SMALL = ImageLoader.CHES_MAPPER_SMALL;
	public static final ImageIcon OPENTOX_ICON = ImageLoader.OPENTOX;

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
	public static String STRUCTURAL_FRAGMENT_DIR = BASE_DIR + File.separator + "structural_fragments";

	static
	{
		for (String d : new String[] { BASE_DIR, STRUCTURAL_FRAGMENT_DIR })
		{
			File dir = new File(d);
			if (!dir.exists())
				dir.mkdir();
			if (!dir.exists())
				throw new Error("Could not create '" + d + "'");
		}
	}

	public static String[] getFragmentFiles()
	{
		String fragments[] = new File(STRUCTURAL_FRAGMENT_DIR).list(new FilenameFilter()
		{

			@Override
			public boolean accept(File dir, String name)
			{
				return name.endsWith(".csv");
			}
		});
		for (int i = 0; i < fragments.length; i++)
			fragments[i] = STRUCTURAL_FRAGMENT_DIR + File.separator + fragments[i];
		return fragments;
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

	public static String getFragmentFileDestination(String string)
	{
		return STRUCTURAL_FRAGMENT_DIR + File.separator + string;
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

	public static String destinationFile(String destinationFilename)
	{
		return BASE_DIR + File.separator + destinationFilename;
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

	// ------------------------ EXTERNAL PROGRAMMS -----------------------

	public static Binary BABEL_BINARY = new Binary("babel", "CM_BABEL_PATH", OPENBABEL_STRING);
	public static Binary RSCRIPT_BINARY = new Binary("Rscript", "CM_RSCRIPT_PATH", R_STRING);

	private static String babelVersion = null;

	public static String getOpenBabelVersion()
	{
		if (!BABEL_BINARY.isFound())
			throw new IllegalStateException();
		if (babelVersion == null)
		{
			try
			{
				File bVersion = File.createTempFile("babel", "version");
				ExternalTool ext = new ExternalTool();
				ext.run("babel", BABEL_BINARY.getLocation() + " -V", bVersion, true);
				BufferedReader b = new BufferedReader(new FileReader(bVersion));
				String s;
				Pattern pattern = Pattern.compile("^.*([0-9]+\\.[0-9]+\\.[0-9]+).*$");
				while ((s = b.readLine()) != null)
				{
					Matcher matcher = pattern.matcher(s);
					if (matcher.matches())
					{
						babelVersion = matcher.group(1);
						break;
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return babelVersion;
	}

	public static String getOBFile(String s)
	{
		if (!BABEL_BINARY.isFound())
			throw new IllegalStateException();
		String p = FileUtil.getParent(BABEL_BINARY.getLocation());
		if (OSUtil.isWindows())
		{
			String f = p + "\\data\\" + s;
			if (new File(f).exists())
				return f;
			throw new Error("not found: " + f);
		}
		else
		{
			//default dir
			String f = "/usr/local/share/openbabel/" + getOpenBabelVersion() + "/" + s;
			if (new File(f).exists())
				return f;
			//hack
			while (p.length() > 1)
			{
				f = p + "/share/openbabel/" + getOpenBabelVersion() + "/" + s;
				if (new File(f).exists())
					return f;
				f = p + "/install/share/openbabel/" + getOpenBabelVersion() + "/" + s;
				if (new File(f).exists())
					return f;
				p = FileUtil.getParent(p);
			}
			throw new Error("not found: " + s);
		}
	}

	private static List<Binary> bins = new ArrayList<Binary>();
	static
	{
		bins.add(BABEL_BINARY);
		bins.add(RSCRIPT_BINARY);
		for (Binary binary : bins)
		{
			String path = (String) Settings.PROPS.get("bin-path-" + binary.getCommand());
			if (path != null)
				binary.setLocation(path);
		}
		locateBinarys();
	}

	public static void locateBinarys()
	{
		BinaryLocator.locate(bins);
	}

	public static void showBinaryDialog(Binary select)
	{
		locateBinarys();
		new BinaryLocatorDialog((Window) Settings.TOP_LEVEL_COMPONENT, "External Programs", TITLE, bins, select);
		for (Binary binary : bins)
			if (binary.getLocation() != null)
				Settings.PROPS.put("bin-path-" + binary.getCommand(), binary.getLocation());
			else
				Settings.PROPS.remove("bin-path-" + binary.getCommand());
		storeProps();
	}

	public static Component getBinaryComponent(final Binary bin)
	{
		final LinkButton l = new LinkButton("Uses external program: " + bin.getDescription());
		l.setForegroundFont(l.getFont().deriveFont(Font.PLAIN));
		l.setSelectedForegroundFont(l.getFont().deriveFont(Font.PLAIN));
		l.setSelectedForegroundColor(Color.BLUE);
		bin.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (bin.isFound())
					l.setIcon(ImageLoader.TOOL);
			}
		});
		if (!bin.isFound())
			l.setIcon(ImageLoader.ERROR);
		else
			l.setIcon(ImageLoader.TOOL);
		l.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Settings.showBinaryDialog(bin);
			}
		});
		return l;
	}
}
