package org.chesmapper.map.property;

import java.io.File;
import java.nio.channels.IllegalSelectorException;
import java.util.LinkedHashMap;
import java.util.Set;

import org.chesmapper.map.dataInterface.CompoundPropertySet.Type;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.util.ExternalToolUtil;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.CollectionUtil;
import org.mg.javalib.util.FileUtil;

public class OBDescriptorFactory
{
	private static LinkedHashMap<String, String> descriptors;

	private static String[] nominal = new String[] {};
	private static String[] numeric = new String[] { "abonds", "atoms", "bonds", "dbonds", "sbonds", "tbonds", "HBA1",
			"HBA2", "HBD", "logP", "MR", "MW", "nF", "nHal", "TPSA" };

	public static Type getDescriptorDefaultType(String id)
	{
		if (ArrayUtil.indexOf(numeric, id) != -1)
			return Type.NUMERIC;
		if (ArrayUtil.indexOf(nominal, id) != -1)
			return Type.NOMINAL;
		else
			return null;
	}

	public synchronized static Set<String> getDescriptorIDs(boolean forceReload)
	{
		if (descriptors == null || forceReload)
		{
			if (!BinHandler.BABEL_BINARY.isFound())
				return null;
			descriptors = new LinkedHashMap<String, String>();
			File descFile = null;
			try
			{
				descFile = File.createTempFile("babel", "desc");
				ExternalToolUtil.run("babel",
						new String[] { BinHandler.BABEL_BINARY.getLocation(), "-L", "descriptors" }, descFile);
				String s = FileUtil.readStringFromFile(descFile.getAbsolutePath());

				for (String s2 : s.split("\n"))
				{
					int i = s2.indexOf(" ");
					String desc = s2.substring(0, i);
					String descDescription = s2.substring(i).trim();
					descriptors.put(desc, descDescription);
				}
				Settings.LOGGER.debug("babel descriptors: " + CollectionUtil.toString(descriptors.keySet()));
			}
			catch (Exception e)
			{
				Settings.LOGGER.error(e);
			}
			finally
			{
				if (descFile != null)
					descFile.delete();
			}
		}
		return descriptors.keySet();
	}

	public static String getDescriptorDescription(String id)
	{
		if (descriptors == null)
			throw new IllegalSelectorException();
		return descriptors.get(id);
	}

	public static String[] compute(String sdfFile, String id)
	{
		if (!BinHandler.BABEL_BINARY.isFound())
			return null;
		descriptors = new LinkedHashMap<String, String>();
		File descFile = null;
		try
		{
			descFile = File.createTempFile("babel", "desc");
			ExternalToolUtil.run("babel", new String[] { BinHandler.BABEL_BINARY.getLocation(), "-isdf", sdfFile,
					"--append", id, "-osmi", descFile.getAbsolutePath() });
			String s = FileUtil.readStringFromFile(descFile.getAbsolutePath());
			String lines[] = s.split("\n");
			String vals[] = new String[lines.length];
			for (int i = 0; i < vals.length; i++)
			{
				int indexTab = lines[i].lastIndexOf("\t");
				int indexSpace = lines[i].lastIndexOf(" ");
				int index = Math.max(indexTab, indexSpace);
				vals[i] = lines[i].substring(index + 1).trim();
			}
			Settings.LOGGER.info(id + " " + ArrayUtil.toString(vals));
			return vals;
		}
		catch (Exception e)
		{
			Settings.LOGGER.error(e);
			return null;
		}
		finally
		{
			if (descFile != null)
				descFile.delete();
		}
	}

	public static void main(String args[])
	{
		//getDescriptors();
		compute("/home/martin/data/PBDE_LogVP.ob3d.sdf", "logP");
	}

}
