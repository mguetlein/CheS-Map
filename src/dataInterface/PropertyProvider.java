package dataInterface;

import java.util.HashMap;

import data.DatasetFile;

public class PropertyProvider
{
	//	private static HashMap<String, DefaultCompoundProperty> uniqueNames = new HashMap<String, DefaultCompoundProperty>();
	//
	//	public static void clearPropertyOfType(Class<?> type)
	//	{
	//		List<String> toDel = new ArrayList<String>();
	//		for (String k : uniqueNames.keySet())
	//			if (uniqueNames.get(k).getClass().equals(type))
	//				toDel.add(k);
	//		for (String k : toDel)
	//			uniqueNames.remove(k);
	//	}
	//	
	//	public static CompoundProperty findOrCreate(String name, String uniqueName, String description);
	//	
	//	public static boolean exists(String name, String uniqueName, String description);

	public static HashMap<String, DefaultCompoundProperty> map = new HashMap<String, DefaultCompoundProperty>();

	public static DefaultCompoundProperty findOrCreate(String name, DatasetFile datasetFile, String description)
	{
		String key = name + "_" + datasetFile.getFullName() + "_" + datasetFile.getMD5();
		if (!map.containsKey(key))
			map.put(key, new DefaultCompoundProperty(name, description));
		return map.get(key);
	}

}
