package data.obdesc;

import java.io.File;
import java.nio.channels.IllegalSelectorException;
import java.util.LinkedHashMap;
import java.util.Set;

import main.Settings;
import util.ArrayUtil;
import util.ExternalToolUtil;
import util.FileUtil;
import dataInterface.MoleculeProperty.Type;

public class OBDescriptorFactory
{
	private static LinkedHashMap<String, String> descriptors;

	private static String[] nominal = new String[] {};
	private static String[] numeric = new String[] { "abonds", "atoms", "bonds", "dbonds", "sbonds", "tbonds", "HBA1",
			"HBA2", "HBD", "L5", "logP", "MR", "MW", "nF", "nHal", "TPSA" };

	public static Type getDescriptorDefaultType(String id)
	{
		if (ArrayUtil.indexOf(numeric, id) != -1)
			return Type.NUMERIC;
		if (ArrayUtil.indexOf(nominal, id) != -1)
			return Type.NOMINAL;
		else
			return null;
	}

	public static Set<String> getDescriptorIDs(boolean forceReload)
	{
		if (descriptors == null || forceReload)
		{
			if (!Settings.BABEL_BINARY.isFound())
				return null;
			descriptors = new LinkedHashMap<String, String>();
			File descFile = null;
			try
			{
				descFile = File.createTempFile("babel", "desc");
				ExternalToolUtil.run("babel", Settings.BABEL_BINARY.getLocation() + " -L descriptors", descFile);
				String s = FileUtil.readStringFromFile(descFile.getAbsolutePath());

				for (String s2 : s.split("\n"))
				{
					int i = s2.indexOf(" ");
					String desc = s2.substring(0, i);
					String descDescription = s2.substring(i).trim();
					descriptors.put(desc, descDescription);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
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
		if (!Settings.BABEL_BINARY.isFound())
			return null;
		descriptors = new LinkedHashMap<String, String>();
		File descFile = null;
		try
		{
			descFile = File.createTempFile("babel", "desc");
			ExternalToolUtil.run("babel", Settings.BABEL_BINARY.getLocation() + " -isdf " + sdfFile + " --append " + id
					+ " -osmi", descFile);
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
			System.out.println(id + " " + ArrayUtil.toString(vals));
			return vals;
		}
		catch (Exception e)
		{
			e.printStackTrace();
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
