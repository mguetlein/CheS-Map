package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import util.FileUtil.CSVFile;

public class ValueFileCache
{
	public static List<String[]> readCacheString(String cacheFile, int expectedSize)
	{
		CSVFile file = FileUtil.readCSV(cacheFile, expectedSize);
		if (file == null)
			throw new Error("could not read csv");
		return file.content;
	}

	public static List<Double[]> readCacheDouble(String cacheFile, int expectedSize)
	{
		List<Double[]> d = new ArrayList<Double[]>();
		for (String[] strings : readCacheString(cacheFile, expectedSize))
			d.add(ArrayUtil.parse(strings));
		return d;
	}

	public static List<Integer[]> readCacheInteger(String cacheFile)
	{
		return readCacheInteger(cacheFile, -1);
	}

	public static List<Integer[]> readCacheInteger(String cacheFile, int expectedSize)
	{
		List<Integer[]> d = new ArrayList<Integer[]>();
		for (String[] strings : readCacheString(cacheFile, expectedSize))
			d.add(ArrayUtil.parseIntegers(strings));
		return d;
	}

	public static List<Integer> readCacheInteger2(String cacheFile, int expectedSize)
	{
		return ArrayUtil.toList(readCacheInteger(cacheFile, expectedSize).get(0));
	}

	private static List<Vector3f[]> readCachePosition(String cacheFile, int expectedSize)
	{
		List<Vector3f[]> d = new ArrayList<Vector3f[]>();
		for (String[] strings : readCacheString(cacheFile, expectedSize))
			d.add(Vector3fUtil.parseArray(strings));
		return d;
	}

	public static List<Vector3f> readCachePosition2(String cacheFile, int expectedSize)
	{
		return ArrayUtil.toList(readCachePosition(cacheFile, expectedSize).get(0));
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

	public static void writeCacheInteger2(String cacheFile, List<Integer> values)
	{
		List<Integer[]> l = new ArrayList<Integer[]>();
		l.add(ArrayUtil.toIntegerArray(values));
		writeCacheInteger(cacheFile, l);
	}

	public static void writeCacheInteger(String cacheFile, List<Integer[]> values)
	{
		List<Object[]> l = new ArrayList<Object[]>();
		for (Integer[] o : values)
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

	public static void writeCachePosition2(String filename, List<Vector3f> positions)
	{
		File f = new File(filename);
		try
		{
			BufferedWriter b = new BufferedWriter(new FileWriter(f));
			String s = "";
			for (Vector3f vector3f : positions)
				s += Vector3fUtil.serialize(vector3f) + ",";
			s = s.substring(0, s.length() - 1);
			b.write(s + "\n");
			b.close();
		}
		catch (IOException e)
		{
			throw new Error(e);
		}
	}

}
