package weka;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.NotImplementedException;

import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;

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

	private CompoundArffWriter(List<MolecularPropertyOwner> compounds, List<MoleculeProperty> features)
	{
		this.compounds = compounds;
		this.features = features;
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
		return features.get(attribute).toString();
	}

	@Override
	public String getAttributeValueSpace(int attribute)
	{
		return "numeric";
	}

	@Override
	public int getNumInstances()
	{
		return compounds.size();
	}

	@Override
	public String getAttributeValue(int instance, int attribute)
	{
		return compounds.get(instance).getValue(features.get(attribute), true).toString();
	}

	@Override
	public boolean isSparse()
	{
		return false;
	}

	@Override
	public String getMissingValue(int attribute)
	{
		throw new NotImplementedException();
	}

	@Override
	public boolean isInstanceWithoutAttributeValues(int instance)
	{
		return false;
	}
}
