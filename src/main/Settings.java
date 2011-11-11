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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import util.FileUtil;
import util.ImageLoader;
import util.OSUtil;
import weka.core.Version;

public class Settings
{
	// -------------------------- GRAPHICAL STUFF ---------------------------------

	public static boolean SCREENSHOT_SETUP = false;

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

	private static ResourceBundle text = ResourceBundle.getBundle("ches-map");

	public static String text(String key)
	{
		return text.getString(key);
	}

	public static String text(String key, String param1)
	{
		return MessageFormat.format(text.getString(key), param1);
	}

	public static String text(String key, String param1, String param2)
	{
		return MessageFormat.format(text.getString(key), param1, param2);
	}

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
	public static String MAJOR_MINOR_VERSION = "v?.?";
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
			MAJOR_MINOR_VERSION = VERSION.substring(0, VERSION.lastIndexOf('.'));
			r.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static String CDK_VERSION = "1.4.4";
	public static String CDK_STRING = text("lib.cdk", CDK_VERSION);
	public static String OPENBABEL_STRING = text("lib.openbabel");
	public static String R_STRING = text("lib.r");
	public static String WEKA_STRING = text("lib.weka", Version.VERSION);

	public static String VERSION_STRING = VERSION + " Initial Prototype"
			+ ((BUILD_DATE != null) ? (", " + BUILD_DATE) : "");
	public static String TITLE = "CheS-Mapper";
	public static String HOMEPAGE = "http://opentox.informatik.uni-freiburg.de/ches-mapper";
	public static String SMSD_STRING = "The Small Molecule Subgraph Detector (SMSD) (see http://www.ebi.ac.uk/thornton-srv/software/SMSD, integrated into CDK)";

	// ------------------ TMP/RESULT-FILE SUPPORT --------------------------------------------- 

	public static String BASE_DIR = System.getProperty("user.home") + File.separator + ".ches-mapper";
	public static String STRUCTURAL_FRAGMENT_DIR = BASE_DIR + File.separator + "structural_fragments";
	public static String MODIFIED_BABEL_DATA_DIR = BASE_DIR + File.separator + "babel_data";
	public static String R_LIB_DIR = BASE_DIR + File.separator + "r_libs";

	static
	{
		for (String d : new String[] { BASE_DIR, STRUCTURAL_FRAGMENT_DIR, MODIFIED_BABEL_DATA_DIR, R_LIB_DIR })
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
				//return name.endsWith(".csv");
				return true;
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
	public static final String PROPERTIES_FILE = BASE_DIR + File.separator + "ches.mapper." + MAJOR_MINOR_VERSION
			+ ".props";

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
			System.out.println("Read properties from: " + PROPERTIES_FILE);
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
				bVersion.delete();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return babelVersion;
	}

	public static String getOBFileModified(String destinationFilename)
	{
		return MODIFIED_BABEL_DATA_DIR + File.separator + destinationFilename;
	}

	public static String getOBFileOrig(String s)
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
		BABEL_BINARY.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				babelVersion = null;
			}
		});
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

	public static JComponent getBinaryComponent(final Binary bin)
	{
		final LinkButton l = new LinkButton("Configure external program: " + bin.getDescription());
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
