package org.chesmapper.map.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.vecmath.Vector3f;

import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.Vector3fUtil;
import org.mg.javalib.util.FileUtil.CSVFile;
import org.mg.javalib.util.FileUtil.UnexpectedNumColsException;

public class ValueFileCache
{
	public static List<String[]> readCacheString(String cacheFile, int expectedNumCols)
			throws UnexpectedNumColsException
	{
		CSVFile file = FileUtil.readCSV(cacheFile, expectedNumCols);
		if (file == null)
			throw new Error("could not read csv");
		return file.content;
	}

	public static List<Double[]> readCacheDouble(String cacheFile, int expectedNumCols)
			throws UnexpectedNumColsException
	{
		List<Double[]> d = new ArrayList<Double[]>();
		for (String[] strings : readCacheString(cacheFile, expectedNumCols))
			d.add(ArrayUtil.parse(strings));
		return d;
	}

	public static List<Double> readCacheDouble2(String cacheFile)
	{
		try
		{
			return ArrayUtil.toList(readCacheDouble(cacheFile, -1).get(0));
		}
		catch (UnexpectedNumColsException e)
		{
			throw new Error("should never happen");
		}
	}

	public static List<Double> readCacheDouble2(String cacheFile, int expectedNumCols)
			throws UnexpectedNumColsException
	{
		return ArrayUtil.toList(readCacheDouble(cacheFile, expectedNumCols).get(0));
	}

	public static List<Integer[]> readCacheInteger(String cacheFile)
	{
		try
		{
			return readCacheInteger(cacheFile, -1);
		}
		catch (UnexpectedNumColsException e)
		{
			throw new Error("should never happen");
		}
	}

	public static List<Integer[]> readCacheInteger(String cacheFile, int expectedNumCols)
			throws UnexpectedNumColsException
	{
		List<Integer[]> d = new ArrayList<Integer[]>();
		for (String[] strings : readCacheString(cacheFile, expectedNumCols))
			d.add(ArrayUtil.parseIntegers(strings));
		return d;
	}

	public static List<Integer> readCacheInteger2(String cacheFile, int expectedNumCols)
			throws UnexpectedNumColsException
	{
		return ArrayUtil.toList(readCacheInteger(cacheFile, expectedNumCols).get(0));
	}

	private static List<Vector3f[]> readCachePosition(String cacheFile, int expectedNumCols)
			throws UnexpectedNumColsException
	{
		List<Vector3f[]> d = new ArrayList<Vector3f[]>();
		for (String[] strings : readCacheString(cacheFile, expectedNumCols))
			d.add(Vector3fUtil.parseArray(strings));
		return d;
	}

	public static List<Vector3f> readCachePosition2(String cacheFile, int expectedNumCols)
			throws UnexpectedNumColsException
	{
		return ArrayUtil.toList(readCachePosition(cacheFile, expectedNumCols).get(0));
	}

	public static void writeCacheString(String cacheFile, String[] values)
	{
		List<Object[]> l = new ArrayList<Object[]>();
		l.add(values);
		writeCache(cacheFile, l);
	}

	public static void writeCacheDouble2(String cacheFile, List<Double> values)
	{
		List<Double[]> l = new ArrayList<Double[]>();
		l.add(ArrayUtil.toDoubleArray(values));
		writeCacheDouble(cacheFile, l);
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

	public static void writeSymmetricMatrix(String distFilename, double[][] d)
	{
		File f = new File(distFilename);
		try
		{
			BufferedWriter b = new BufferedWriter(new FileWriter(f));
			b.write(String.valueOf(d.length));
			b.write('\n');
			for (int i = 0; i < d.length - 1; i++)
			{
				for (int j = i + 1; j < d.length; j++)
				{
					b.write(String.valueOf(d[i][j]));
					b.write(';');
				}
				b.write('\n');
			}
			b.close();
		}
		catch (IOException e)
		{
			throw new Error(e);
		}
	}

	public static double[][] readSymmetricMatrix(String distFilename)
	{
		File f = new File(distFilename);
		try
		{
			BufferedReader r = new BufferedReader(new FileReader(f));
			int size = Integer.parseInt(r.readLine());
			double d[][] = new double[size][size];
			for (int i = 0; i < d.length - 1; i++)
			{
				String s[] = r.readLine().split(";");
				int k = 0;
				for (int j = i + 1; j < d.length; j++)
				{
					d[i][j] = Double.parseDouble(s[k++]);
					d[j][i] = d[i][j];
				}
			}
			r.close();
			return d;
		}
		catch (IOException e)
		{
			throw new Error(e);
		}
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
			StringBuffer s = new StringBuffer();
			for (Vector3f vector3f : positions)
			{
				s.append(Vector3fUtil.serialize(vector3f));
				s.append(",");
			}
			String ss = s.toString();
			ss = ss.substring(0, ss.length() - 1);
			b.write(ss + "\n");
			b.close();
		}
		catch (IOException e)
		{
			throw new Error(e);
		}
	}

	public static void main(String[] args) throws IOException
	{
		Random r = new Random();
		//		List<Vector3f> vecs = new ArrayList<Vector3f>();
		//		for (int i = 0; i < 10000; i++)
		//			vecs.add(new Vector3f(r.nextFloat(), r.nextFloat(), r.nextFloat()));
		//		//Vector3f f[] = ArrayUtil.toArray(vecs);
		//		File f = File.createTempFile("bla", "blub");
		//		StopWatchUtil.start("write");
		//		writeCachePosition2(f.getAbsolutePath(), vecs);
		//		//ArrayUtil.toCSVString(f);
		//		StopWatchUtil.stop("write");
		//		StopWatchUtil.print();
		//		System.out.println(f);
		//		//		f.delete();

		int n = 10;
		double d[][] = new double[n][n];
		for (int i = 0; i < d.length - 1; i++)
		{
			for (int j = i + 1; j < d.length; j++)
			{
				d[i][j] = r.nextDouble();
				d[j][i] = d[i][j];
			}
		}
		writeSymmetricMatrix("/tmp/delme", d);
		double d2[][] = readSymmetricMatrix("/tmp/delme");
		for (int i = 0; i < d.length; i++)
			for (int j = 0; j < d.length; j++)
				if (d[i][j] != d2[i][j])
					throw new Error();
	}
}
