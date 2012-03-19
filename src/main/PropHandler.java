package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class PropHandler
{
	public static void init(boolean loadProperties)
	{
		if (INSTANCE != null)
			throw new IllegalStateException("only init once!");
		INSTANCE = new PropHandler(loadProperties);
	}

	private static PropHandler INSTANCE;

	private static PropHandler instance()
	{
		if (INSTANCE == null)
			init(true);
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

	// ---------------------------------------------------------

	private Properties props;
	private String propertiesFile;

	private PropHandler(boolean loadProperties)
	{
		// try loading props
		props = new Properties();
		if (loadProperties)
		{
			propertiesFile = Settings.BASE_DIR + File.separator + "ches.mapper." + Settings.MAJOR_MINOR_VERSION
					+ ".props";
			try
			{
				FileInputStream in = new FileInputStream(propertiesFile);
				props.load(in);
				in.close();
				// System.out.println("property-keys: " + CollectionUtil.toString(PROPS.keySet()));
				// System.out.println("property-values: " + CollectionUtil.toString(PROPS.values()));
				System.out.println("Read properties from: " + propertiesFile);
			}
			catch (Exception e)
			{
				Status.WARN.println("Could not load properties: " + propertiesFile);
			}
		}
		else
			System.out.println("No properties read or stored");
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
				e.printStackTrace();
			}
		}
	}

}
