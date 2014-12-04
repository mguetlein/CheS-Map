package org.chesmapper.map.weka;

import java.io.File;
import java.util.List;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.CompoundData;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.NumericProperty;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.mg.javalib.weka.ArffWritable;
import org.mg.javalib.weka.ArffWriter;

public class CompoundArffWriter implements ArffWritable
{
	public static File writeArffFile(DatasetFile dataset, List<CompoundData> compounds, List<CompoundProperty> features)
	{
		return writeArffFile(dataset.getFeatureTableFilePath("arff"), compounds, features);
	}

	public static File writeArffFile(String arffFile, List<CompoundData> compounds, List<CompoundProperty> features)
	{
		File file = new File(arffFile);
		if (!Settings.CACHING_ENABLED || !file.exists())
		{
			TaskProvider.debug("writing arff file: " + arffFile);
			try
			{
				ArffWriter.writeToArffFile(file, new CompoundArffWriter(compounds, features));
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
		else
			TaskProvider.debug("arff file already exists: " + arffFile);
		return file;
	}

	List<CompoundData> compounds;
	List<CompoundProperty> features;
	boolean sparse = true;

	private CompoundArffWriter(List<CompoundData> compounds, List<CompoundProperty> features)
	{
		this.compounds = compounds;
		this.features = features;

		for (CompoundProperty p : features)
			if (p instanceof NumericProperty)
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
		//return features.get(attribute).getUniqueName();
		return features.get(attribute).getName() + "_" + features.get(attribute).hashCode();
	}

	@Override
	public String getAttributeValueSpace(int attribute)
	{
		if (features.get(attribute) instanceof NumericProperty)
			return "numeric";
		else
		{
			String s = "{";
			for (String o : ((NominalProperty) features.get(attribute)).getDomain())
			{
				if (o != null && o.length() > 1)
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
		if (features.get(attribute) instanceof NumericProperty)
		{
			Double v = compounds.get(instance).getNormalizedValueCompleteDataset(
					(NumericProperty) features.get(attribute));
			if (v == null)
				return "?";
			else
				return v.toString();
		}
		else
		{
			String s = compounds.get(instance).getStringValue((NominalProperty) features.get(attribute));
			if (s == null)
				return "?";
			else if (s.length() > 1)
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
