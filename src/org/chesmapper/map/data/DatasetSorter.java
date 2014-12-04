package org.chesmapper.map.data;

import java.util.Comparator;

import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.StringUtil;

public class DatasetSorter
{
	public String files[];
	public String names[];

	public DatasetSorter(String files[])
	{
		String filenames[] = new String[files.length];
		for (int i = 0; i < filenames.length; i++)
		{
			String n = files[i];
			int sepIndex = n.lastIndexOf("/");
			if (sepIndex == -1)
				sepIndex = n.lastIndexOf("\\");
			if (sepIndex != -1)
			{
				n = n.substring(sepIndex + 1);
				sepIndex = n.indexOf('.');
				if (sepIndex != -1)
					n = n.substring(0, sepIndex);
			}
			filenames[i] = n;
		}
		int order[] = ArrayUtil.getOrdering(filenames, new Comparator<String>()
		{
			@Override
			public int compare(String o1, String o2)
			{
				return StringUtil.compareFilenames(o1, o2);
			}
		}, true);

		this.files = new String[order.length];
		this.names = new String[order.length];
		for (int i = 0; i < order.length; i++)
		{
			this.files[i] = files[order[i]];
			this.names[i] = filenames[order[i]];
		}
	}
}
