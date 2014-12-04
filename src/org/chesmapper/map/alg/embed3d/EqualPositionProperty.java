package org.chesmapper.map.alg.embed3d;

import java.util.LinkedHashMap;
import java.util.List;

import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.chesmapper.map.dataInterface.DefaultNominalProperty;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.StringUtil;

public class EqualPositionProperty extends DefaultNominalProperty
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

	public static EqualPositionProperty create(List<Vector3f> positions)
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

		//		if (!map.containsKey(uniqNameSuffix))
		//			map.put(uniqNameSuffix, new EqualPositionProperty(data, ids, uniqNameSuffix));
		//		return map.get(uniqNameSuffix);

		return new EqualPositionProperty(ids, numDistinctPos, numMultiCompounds, numCommonPos);
	}

	//	private static HashMap<String, EqualPositionProperty> map = new HashMap<String, EqualPositionProperty>();

	int numDistinctPositions;

	private EqualPositionProperty(String ids[], int numDistinctPositions, int numMultiCompounds, int numCommonPos)
	{
		super(Settings.text("props.eq-pos"), Settings.text("props.eq-pos.desc", numMultiCompounds + "", numCommonPos
				+ ""), ids);
		this.numDistinctPositions = numDistinctPositions;
		//		setStringValues(data, ids);
	}

	@Override
	public String getFormattedNullValue()
	{
		return "unique";
	}

	public int numDistinct3DPositions()
	{
		return numDistinctPositions;
	}
}
