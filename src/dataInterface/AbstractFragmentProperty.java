package dataInterface;

import java.util.HashMap;

import data.DatasetFile;
import data.fragments.MatchEngine;

public abstract class AbstractFragmentProperty extends AbstractCompoundProperty
{
	public AbstractFragmentProperty(String name, String description, String smarts, MatchEngine matchEngine)
	{
		this(name, name, description, smarts, matchEngine);
	}

	public AbstractFragmentProperty(String name, String uniqueName, String description, String smarts,
			MatchEngine matchEngine)
	{
		super(name, uniqueName, description);
		setSmarts(smarts);
		setSmartsMatchEngine(matchEngine);
		setTypeAllowed(Type.NUMERIC, false);
		setType(Type.NOMINAL);
	}

	private HashMap<DatasetFile, Integer> freq = new HashMap<DatasetFile, Integer>();

	public void setFrequency(DatasetFile d, int f)
	{
		freq.put(d, f);
	}

	public int getFrequency(DatasetFile d)
	{
		return freq.get(d);
	}
}
