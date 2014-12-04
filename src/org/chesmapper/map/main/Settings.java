package org.chesmapper.map.main;

import java.awt.Font;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Random;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.InsetsUIResource;

import org.chesmapper.map.data.DatasetFile;
import org.mg.javalib.io.Logger;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.ImageLoader;
import org.mg.javalib.util.ScreenUtil;
import org.mg.javalib.util.Version;

public class Settings
{
	static
	{
		try
		{
			Font font = new Font("Dialog", Font.PLAIN, (int) ScreenSetup.INSTANCE.getFontSize());
			UIDefaults uiDefaults = UIManager.getDefaults();
			String comps[] = { "Label", "CheckBox", "List", "RadioButton", "Table", "TextField", "Button", "TextArea",
					"Tree", "ToggleButton", "ComboBox", "Spinner", "TextPane", "Panel", "PopupMenu", "OptionPane",
					"ScrollPane", "MenuBar", "FormattedTextField", "MenuItem", "Menu", "CheckBoxMenuItem",
					"RadioButtonMenuItem" };
			for (String s : comps)
			{
				int style;
				if (s.equals("Label") || s.equals("RadioButton") || s.equals("List") || s.equals("CheckBox"))
					style = Font.PLAIN;
				else
				{
					Font f = uiDefaults.getFont(s + ".font");
					style = f.getStyle();
				}
				uiDefaults.put(s + ".font", font.deriveFont(style));
			}

			if (ScreenSetup.INSTANCE.getFontSize() < 12)
			{
				uiDefaults.put("Button.margin", new InsetsUIResource(2, 8, 2, 8));
			}
		}
		catch (Exception e)
		{
			if (Settings.LOGGER == null)
				e.printStackTrace();
			else
				Settings.LOGGER.error(e);
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

	public static String text(String key, String param1, String param2, String param3)
	{
		return MessageFormat.format(text.getString(key), param1, param2, param3);
	}

	public static String text(String key, String param1, String param2, String param3, String param4)
	{
		return MessageFormat.format(text.getString(key), param1, param2, param3, param4);
	}

	public static JFrame TOP_LEVEL_FRAME = null;
	public static int TOP_LEVEL_FRAME_SCREEN = ScreenUtil.getLargestScreen();
	public static Random RANDOM = new Random();
	public static Boolean DBG = false;
	public static Boolean CACHING_ENABLED = true;
	public static Boolean DESC_MIXTURE_HANDLING = false;
	public static boolean SKIP_REDUNDANT_FEATURES = false;
	public static boolean BIG_DATA = false;

	public static ImageIcon CHES_MAPPER_IMAGE = ImageLoader.getImage(ImageLoader.Image.ches_mapper);
	public static ImageIcon CHES_MAPPER_ICON = ImageLoader.getImage(ImageLoader.Image.ches_mapper_icon);
	public static ImageIcon OPENTOX_IMAGE = ImageLoader.getImage(ImageLoader.Image.opentox);

	static
	{
		if (ScreenSetup.INSTANCE.getWizardSize().getWidth() <= 800)
		{
			CHES_MAPPER_IMAGE = new ImageIcon(Settings.CHES_MAPPER_IMAGE.getImage().getScaledInstance(-1, 100,
					Image.SCALE_SMOOTH));
			OPENTOX_IMAGE = new ImageIcon(Settings.OPENTOX_IMAGE.getImage().getScaledInstance(80, -1,
					Image.SCALE_SMOOTH));
		}
	}

	// ------------------ TMP/RESULT-FILE SUPPORT -------------------------------------------

	public static String BUILD_DATE = null;
	static
	{
		// ADD TO BUILD SCRIPT:
		// <tstamp>
		// <format property="TODAY" pattern="yyyy-MM-dd HH:mm" />
		// </tstamp>
		// <manifest>
		// <attribute name="Built-Date" value="${TODAY}"/>
		// </manifest>
		try
		{
			URL manifestUrl = Settings.class.getResource("/META-INF/MANIFEST.MF");
			// Settings.LOGGER.println(manifestUrl);
			BufferedReader br = new BufferedReader(new InputStreamReader(manifestUrl.openStream()));
			String line;
			while ((line = br.readLine()) != null)
			{
				// Settings.LOGGER.println(line);
				if (line.matches("(?i).*built.*date.*"))
				{
					Settings.BUILD_DATE = line;
					break;
				}
			}
			// Settings.LOGGER.println(BUILD_DATE);
		}
		catch (Exception e)
		{
			// Settings.LOGGER.error(e);
		}
	}

	public static String BASE_DIR = System.getProperty("user.home") + File.separator + ".ches-mapper";
	public static String CACHE_DIR = BASE_DIR + File.separator + "cache";
	public static String STRUCTURAL_FRAGMENT_DIR = BASE_DIR + File.separator + "structural_fragments";
	public static String MODIFIED_BABEL_DATA_DIR = BASE_DIR + File.separator + "babel_data";
	public static String R_LIB_DIR = BASE_DIR + File.separator + "r_libs";
	public static String BABEL_3D_CACHE = BASE_DIR + File.separator + "babel3d";

	public static final Logger LOGGER;
	public static String LOG_FILE = BASE_DIR + File.separator + "ches-mapper.log";

	static
	{
		String legal_dirs[] = new String[] { BASE_DIR, CACHE_DIR, STRUCTURAL_FRAGMENT_DIR, MODIFIED_BABEL_DATA_DIR,
				R_LIB_DIR, BABEL_3D_CACHE };
		String legal_files[] = new String[] { LOG_FILE };

		for (String d : legal_dirs)
		{
			File dir = new File(d);
			if (!dir.exists())
				dir.mkdir();
			if (!dir.exists())
				throw new Error("Could not create '" + d + "'");
		}
		//migration: delete old cache
		for (File f : new File(BASE_DIR).listFiles())
			if (f.isDirectory())
			{
				if (ArrayUtil.indexOf(legal_dirs, f.getAbsolutePath()) == -1)
					FileUtil.deleteDirectory(f);
			}
			else
			{
				if (!PropHandler.isPropFile(f.getName()) && ArrayUtil.indexOf(legal_files, f.getAbsolutePath()) == -1)
					f.delete();
			}

		try
		{
			LOGGER = new Logger(LOG_FILE, true);
			LOGGER.logFromStdErr();
			LOGGER.info("Logger initialized ('" + LOG_FILE + "')");
		}
		catch (RuntimeException e)
		{
			throw new Error("cannot init logger", e);
		}
	}

	public static org.mg.javalib.util.Version VERSION = null;
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
			VERSION = org.mg.javalib.util.Version.fromString(r.readLine());
			r.close();
		}
		catch (Exception e)
		{
			VERSION = new Version(0, 0, 0);
			Settings.LOGGER.error(e);
		}
	}

	public static String MOSS_VERSION = "version 6.10, 2013.04.22";
	public static String CDK_VERSION = "1.4.18";
	public static String CDK_STRING = text("lib.cdk", CDK_VERSION);
	public static boolean CDK_SKIP_SOME_DESCRIPTORS = true;
	public static String OPENBABEL_STRING = text("lib.openbabel");
	public static String R_STRING = text("lib.r");
	public static String FMINER_STRING = text("lib.fminer");
	public static String WEKA_STRING = text("lib.weka", weka.core.Version.VERSION);

	public static String VERSION_STRING = VERSION + ((BUILD_DATE != null) ? (", " + BUILD_DATE) : "");
	public static String TITLE = "CheS-Mapper";
	public static String HOMEPAGE = "http://ches-mapper.org";
	public static String HOMEPAGE_RUNTIME = "http://opentox.informatik.uni-freiburg.de/ches-mapper-wiki/index.php?title=Supported_Formats,_Dataset_Size_and_Algorithm_Runtimes";
	public static String HOMEPAGE_DOCUMENTATION = "http://opentox.informatik.uni-freiburg.de/ches-mapper-wiki";
	public static String HOMEPAGE_SPECIFICITY = "http://opentox.informatik.uni-freiburg.de/ches-mapper-wiki/index.php?title=Working_with_the_3D_Viewer#Sorting_of_feature_values";
	public static String HOMEPAGE_FORMATS = "http://opentox.informatik.uni-freiburg.de/ches-mapper-wiki/index.php?title=Supported_Dataset_Formats_and_Size";
	public static String HOMEPAGE_EXPORT_SETTINGS = "http://opentox.informatik.uni-freiburg.de/ches-mapper-wiki/index.php?title=Export/Import_Wizard_Settings";

	public static String CONTACT = "Martin Guetlein (ches-mapper@informatik.uni-freiburg.de)";

	// ------------------ TMP/RESULT-FILE SUPPORT ---------------------------------------------

	public static String[] getFragmentFiles()
	{
		String fragments[] = new File(STRUCTURAL_FRAGMENT_DIR).list(new FilenameFilter()
		{

			@Override
			public boolean accept(File dir, String name)
			{
				// return name.endsWith(".csv");
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
			return CACHE_DIR + File.separator + URLEncoder.encode(url, "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			Settings.LOGGER.error(e);
			return null;
		}
	}

	public static String getFragmentFileDestination(String string)
	{
		return STRUCTURAL_FRAGMENT_DIR + File.separator + string;
	}

	public static String destinationFile(DatasetFile dataset, String filenameSuffix)
	{
		return destinationFile(dataset.getShortName() + "." + dataset.getMD5() + "." + filenameSuffix);
	}

	public static String destinationFile(String destinationFilename)
	{
		return CACHE_DIR + File.separator + destinationFilename;
	}

	// ------------------------ EXTERNAL PROGRAMMS -----------------------

}
