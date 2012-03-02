package dataInterface;

import java.util.HashMap;

import data.DatasetFile;

public abstract class AbstractFragmentProperty extends AbstractMoleculeProperty
{
	public AbstractFragmentProperty(String name, String description, String smarts)
	{
		this(name, name, description, smarts);
	}

	public AbstractFragmentProperty(String name, String uniqueName, String description, String smarts)
	{
		super(name, uniqueName, description);
		setSmarts(smarts);
		setTypeAllowed(Type.NUMERIC, false);
		setType(Type.NOMINAL);
		setNominalDomain(new String[] { "0", "1" });
	}

	private HashMap<DatasetFile, Integer> freq = new HashMap<DatasetFile, Integer>();

	public void setFrequency(DatasetFile d, int f)
	{
		freq.put(d, f);
	}

	public int getFrequency(DatasetFile d)
	{
		if (freq.get(d) == null)
			return 0;
		return freq.get(d);
	}
}
