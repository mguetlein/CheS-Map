package weka;

import java.io.File;
import java.util.List;

import main.Settings;
import util.FileUtil;
import data.DatasetFile;
import dataInterface.MolecularPropertyOwner;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import dataInterface.MoleculePropertyUtil;

public class CompoundArffWriter implements ArffWritable
{
	public static File writeArffFile(DatasetFile dataset, List<MolecularPropertyOwner> compounds,
			List<MoleculeProperty> features)
	{
		String enc = MoleculePropertyUtil.getSetMD5(features, dataset.getMD5());
		String arffFile = Settings.destinationFile(dataset.getSDFPath(false),
				FileUtil.getFilename(dataset.getSDFPath(false)) + "." + enc + ".arff");
		File file = new File(arffFile);
		if (!file.exists())
		{
			System.out.println("writing arff file: " + arffFile);
			ArffWriter.writeToArffFile(file, new CompoundArffWriter(compounds, features));
		}
		else
			System.out.println("arff file already exists: " + arffFile);
		return file;
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
			for (String o : features.get(attribute).getNominalDomain())
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
		if (features.get(attribute).getType() == Type.NUMERIC)
			return compounds.get(instance).getNormalizedValue(features.get(attribute)) + "";
		else
		{
			String s = compounds.get(instance).getStringValue(features.get(attribute));
			if (s != null && s.length() > 1)
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
