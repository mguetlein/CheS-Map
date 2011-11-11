package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import util.FileUtil.CSVFile;

public class ValueFileCache
{

	public static List<String[]> readCacheString(String cacheFile)
	{
		CSVFile file = FileUtil.readCSV(cacheFile);
		if (file == null)
			throw new Error("could not read csv");
		return file.content;
	}

	public static List<Double[]> readCacheDouble(String cacheFile)
	{
		List<Double[]> d = new ArrayList<Double[]>();
		for (String[] strings : readCacheString(cacheFile))
			d.add(ArrayUtil.parse(strings));
		return d;
	}

	public static void writeCacheString(String cacheFile, String[] values)
	{
		List<Object[]> l = new ArrayList<Object[]>();
		l.add(values);
		writeCache(cacheFile, l);
	}

	public static void writeCacheDouble(String cacheFile, List<Double[]> values)
	{
		List<Object[]> l = new ArrayList<Object[]>();
		for (Double[] o : values)
			l.add(o);
		writeCache(cacheFile, l);
	}

	public static void writeCache(String cacheFile, List<Object[]> values)
	{
		File f = new File(cacheFile);
		try
		{
			BufferedWriter b = new BufferedWriter(new FileWriter(f));
			for (Object[] objects : values)
			{
				b.write(ArrayUtil.toCSVString(objects) + "\n");
			}
			b.close();
		}
		catch (IOException e)
		{
			throw new Error(e);
		}
	}
}
