package dataInterface;

import java.util.HashMap;

import data.DatasetFile;

public abstract class AbstractFragmentProperty extends AbstractMoleculeProperty
{
	public AbstractFragmentProperty(String name, String description, String smarts)
	{
		super(name, description);
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
		return freq.get(d);
	}
}
