package weka;

import java.io.File;
import java.io.IOException;
import java.util.List;

import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;

public class CompoundArffWriter implements ArffWritable
{
	public static File writeArffFile(List<MolecularPropertyOwner> compounds, List<MoleculeProperty> features)
	{
		try
		{
			File f = File.createTempFile("compounds", "arff");
			ArffWriter.writeToArffFile(f, new CompoundArffWriter(compounds, features));
			return f;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	List<MolecularPropertyOwner> compounds;
	List<MoleculeProperty> features;
	boolean sparse = true;

	private CompoundArffWriter(List<MolecularPropertyOwner> compounds, List<MoleculeProperty> features)
	{
		this.compounds = compounds;
		this.features = features;

		for (MoleculeProperty p : features)
			if (p.getType() == Type.NUMERIC)
			{
				sparse = false;
				break;
			}
		if (features.size() < 100)
			sparse = false;
	}

	@Override
	public List<String> getAdditionalInfo()
	{
		return null;
	}

	@Override
	public int getNumAttributes()
	{
		return features.size();
	}

	@Override
	public String getAttributeName(int attribute)
	{
		return features.get(attribute).getUniqueName();
	}

	@Override
	public String getAttributeValueSpace(int attribute)
	{
		if (features.get(attribute).getType() == Type.NUMERIC)
			return "numeric";
		else
		{
			String s = "{";
			for (Object o : features.get(attribute).getNominalDomain())
			{
				if (o.toString().length() > 1)
					s += "\"" + o + "\",";
				else
					s += o + ",";
			}
			s = s.substring(0, s.length() - 1);
			s += "}";
			return s;
		}
	}

	@Override
	public int getNumInstances()
	{
		return compounds.size();
	}

	@Override
	public String getAttributeValue(int instance, int attribute)
	{
		if (features.get(attribute).getType() == Type.NUMERIC)
			return compounds.get(instance).getNormalizedValue(features.get(attribute)) + "";
		else
		{
			String s = compounds.get(instance).getStringValue(features.get(attribute));
			if (s.length() > 1)
				return "\"" + s + "\"";
			else
				return s;
		}
	}

	@Override
	public boolean isSparse()
	{
		return sparse;
	}

	@Override
	public String getMissingValue(int attribute)
	{
		return "?";
	}

	@Override
	public boolean isInstanceWithoutAttributeValues(int instance)
	{
		return false;
	}
}
