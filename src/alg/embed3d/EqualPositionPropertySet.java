package alg.embed3d;

import gui.binloc.Binary;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import main.Settings;
import main.TaskProvider;
import util.ArrayUtil;
import util.StringUtil;
import data.DatasetFile;
import dataInterface.AbstractCompoundProperty;
import dataInterface.CompoundProperty;
import dataInterface.CompoundPropertySet;

public class EqualPositionPropertySet extends AbstractCompoundProperty implements CompoundPropertySet
{
	static class MyVector3f extends Vector3f // to fix missing overwrite of equals(Object) in old vecmatch lib included in the cdk 1.14.18
	{
		public MyVector3f(Tuple3f v)
		{
			super(v);
		}

		@Override
		public boolean equals(Object o)
		{
			return o instanceof MyVector3f && super.equals(this);
		}
	}

	public static EqualPositionPropertySet create(DatasetFile data, List<Vector3f> positions, String uniqNameSuffix)
	{
		int cCount = 0;
		LinkedHashMap<MyVector3f, List<Integer>> posMap = new LinkedHashMap<MyVector3f, List<Integer>>();
		for (Vector3f v : positions)
		{
			MyVector3f w = new MyVector3f(v);
			if (posMap.containsKey(w))
				posMap.get(w).add(cCount);
			else
				posMap.put(w, ArrayUtil.toList(new int[] { cCount }));
			cCount++;
		}
		int numDistinctPos = posMap.size();
		if (numDistinctPos == cCount)
			return null; // all positions unique, return null!

		int numMultiCompounds = 0;
		int numCommonPos = 0;
		for (List<Integer> l : posMap.values())
			if (l.size() > 1)
			{
				numCommonPos++;
				numMultiCompounds += l.size();
			}
		TaskProvider.warning(Settings.text("eq-pos.warning", cCount + "", numDistinctPos + ""),
				Settings.text("eq-pos.warning.details", numMultiCompounds + "", numCommonPos + ""));

		int id = 0;
		String ids[] = new String[positions.size()];
		for (MyVector3f v : posMap.keySet())
		{
			if (posMap.get(v).size() > 1)
			{
				// feature values are P<N>, fill up with zeros for sorting
				String p = "P" + StringUtil.concatChar((id + 1) + "", (numCommonPos + "").length(), '0', false);
				for (Integer idx : posMap.get(v))
					ids[idx] = p;
				id++;
			}
		}

		if (!map.containsKey(uniqNameSuffix))
			map.put(uniqNameSuffix, new EqualPositionPropertySet(data, ids, uniqNameSuffix));
		return map.get(uniqNameSuffix);
	}

	private static HashMap<String, EqualPositionPropertySet> map = new HashMap<String, EqualPositionPropertySet>();

	private EqualPositionPropertySet(DatasetFile data, String ids[], String uniqNameSuffix)
	{
		super(Settings.text("props.eq-pos"), "eq-pos." + uniqNameSuffix, Settings.text("props.eq-pos.desc"));
		setStringValues(data, ids);
	}

	@Override
	public String getFormattedNullValue()
	{
		return "unique";
	}

	@Override
	public CompoundPropertySet getCompoundPropertySet()
	{
		return this;
	}

	@Override
	public boolean isComputed(DatasetFile dataset)
	{
		return true;
	}

	@Override
	public boolean isCached(DatasetFile dataset)
	{
		return true;
	}

	@Override
	public boolean compute(DatasetFile dataset)
	{
		return true;
	}

	@Override
	public boolean isSizeDynamic()
	{
		return false;
	}

	@Override
	public boolean isSizeDynamicHigh(DatasetFile dataset)
	{
		return false;
	}

	@Override
	public int getSize(DatasetFile d)
	{
		return 1;
	}

	@Override
	public CompoundProperty get(DatasetFile d, int index)
	{
		return this;
	}

	@Override
	public String getDescription()
	{
		return description;
	}

	@Override
	public Type getType()
	{
		return Type.NOMINAL;
	}

	@Override
	public Binary getBinary()
	{
		return null;
	}

	@Override
	public boolean isSelectedForMapping()
	{
		return false;
	}

	@Override
	public String getNameIncludingParams()
	{
		return name;
	}

	@Override
	public boolean isComputationSlow()
	{
		return false;
	}

	@Override
	public boolean isSensitiveTo3D()
	{
		return false;
	}
}
