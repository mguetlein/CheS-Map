package org.chesmapper.map.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Properties;

import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.Version;

public class PropHandler
{
	public static void init(boolean loadProperties)
	{
		if (INSTANCE != null)
			throw new IllegalStateException("only init once!");
		INSTANCE = new PropHandler(loadProperties);
	}

	public static void forceReload()
	{
		INSTANCE = new PropHandler(true);
	}

	private static PropHandler INSTANCE;

	private static PropHandler instance()
	{
		if (INSTANCE == null)
			throw new IllegalStateException("init first!");
		return INSTANCE;
	}

	public static String get(String property)
	{
		return (String) instance().props.get(property);
	}

	public static String put(String property, String value)
	{
		return (String) instance().props.put(property, value);
	}

	public static String remove(String property)
	{
		return (String) instance().props.remove(property);
	}

	public static Properties getProperties()
	{
		return instance().props;
	}

	public static String getPropertiesFile()
	{
		return instance().propertiesFile;
	}

	public static void storeProperties()
	{
		instance().storeProps();
	}

	public static long modificationTime()
	{
		if (instance().propertiesFile != null)
			return new File(instance().propertiesFile).lastModified();
		else
			return -1;
	}

	// ---------------------------------------------------------

	private Properties props;
	private String propertiesFile;

	private static boolean isPropertyCompatible(Version version1, Version version2)
	{
		if (version1.major != version2.major)
		{
			//different major versions are incompatible
			return false;
		}
		else if (version1.major == 0)
		{
			// no prop-compatibility-support for version 0
			return false;
		}
		else if (version1.major == 1 || version1.major == 2)
		{
			//so far all minor versions within major version 1 are compatible
			return true;
		}
		else
		{
			// let minors by default be compatible in higher versions 
			Settings.LOGGER.warn("No compatibility info for versions > 1");
			return true;
		}
	}

	public static boolean isPropFile(String name)
	{
		return name.matches("ches\\.mapper\\.v[0-9]+\\.[0-9]+\\.props");
	}

	private PropHandler(boolean loadProperties)
	{
		// try loading props
		props = new Properties();
		if (loadProperties)
		{
			String propFileNames[] = new File(Settings.BASE_DIR).list(new FilenameFilter()
			{
				@Override
				public boolean accept(File dir, String name)
				{
					return isPropFile(name);
				}
			});
			propertiesFile = Settings.BASE_DIR + File.separator + "ches.mapper.v" + Settings.VERSION.major + "."
					+ Settings.VERSION.minor + ".props";

			if (new File(propertiesFile).exists())
				Settings.LOGGER.debug("Property file for current version " + Settings.VERSION + " found: "
						+ propertiesFile);
			else
			{
				Settings.LOGGER.debug("Property file for current version " + Settings.VERSION + " not found");
				if (propFileNames.length > 0)
				{
					Settings.LOGGER.debug(propFileNames.length + " other property files found");
					Version mmVersions[] = new Version[propFileNames.length];
					for (int i = 0; i < mmVersions.length; i++)
					{
						String majorMinor = propFileNames[i].substring("ches.mapper.".length(),
								propFileNames[i].indexOf(".props"));
						mmVersions[i] = Version.fromMajorMinorString(majorMinor);
					}
					Version currentMajorMinor = new Version(Settings.VERSION.major, Settings.VERSION.minor);
					if (ArrayUtil.indexOf(mmVersions, currentMajorMinor) != -1)
						throw new Error("internal error, current prop file should not be included");
					int order[] = ArrayUtil.getOrdering(mmVersions, Version.COMPARATOR, false);
					propFileNames = ArrayUtil.sortAccordingToOrdering(order, propFileNames);
					mmVersions = ArrayUtil.sortAccordingToOrdering(order, mmVersions);
					int complientIndex = -1;
					for (int i = 0; i < mmVersions.length; i++)
					{
						if (isPropertyCompatible(currentMajorMinor, mmVersions[i]))
						{
							Settings.LOGGER.debug(Settings.VERSION + " is complient with " + propFileNames[i]);
							complientIndex = i;
							break;
						}
					}
					if (complientIndex != -1)
					{
						String source = Settings.BASE_DIR + File.separator + propFileNames[complientIndex];
						Settings.LOGGER.debug("Copy " + source + " to " + propertiesFile);
						FileUtil.copy(new File(source), new File(propertiesFile));
					}
					else
						Settings.LOGGER.debug("No compatible property file found");
				}
			}

			//migration
			for (String f : propFileNames)
				if (!propertiesFile.endsWith(f))
					new File(Settings.BASE_DIR + File.separator + f).delete();

			try
			{
				FileInputStream in = new FileInputStream(propertiesFile);
				props.load(in);
				in.close();
				// Settings.LOGGER.println("property-keys: " + CollectionUtil.toString(PROPS.keySet()));
				// Settings.LOGGER.println("property-values: " + CollectionUtil.toString(PROPS.values()));
				Settings.LOGGER.info("Read properties from: " + propertiesFile);
			}
			catch (Exception e)
			{
				Status.WARN.println("Could not load properties: " + propertiesFile);
			}
		}
		else
			Settings.LOGGER.info("No properties read or stored");
	}

	private void storeProps()
	{
		if (propertiesFile != null)
		{
			try
			{
				FileOutputStream out = new FileOutputStream(propertiesFile);
				props.store(out, "---No Comment---");
				out.close();
			}
			catch (Exception e)
			{
				Settings.LOGGER.error(e);
			}
		}
	}

	public static boolean containsKey(String prop)
	{
		return INSTANCE.props.containsKey(prop);
	}

}
