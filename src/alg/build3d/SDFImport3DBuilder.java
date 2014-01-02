package alg.build3d;

import gui.Messages;
import gui.binloc.Binary;
import gui.property.FileProperty;
import gui.property.Property;
import gui.property.StringProperty;
import io.SDFUtil;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.openscience.cdk.ChemFile;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

import util.ArrayUtil;
import alg.AlgorithmException.ThreeDBuilderException;
import data.DatasetFile;
import data.FeatureService.SDFReader;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;

public class SDFImport3DBuilder extends AbstractReal3DBuilder
{
	public static final ThreeDBuilder INSTANCE = new SDFImport3DBuilder();

	FileProperty sdfFile = new FileProperty("SDF file", null);
	StringProperty idFeatureDataset = new StringProperty("ID feature in dataset", "id");
	StringProperty idFeatureSDF = new StringProperty("ID feature in import SDF", "DSSTox_RID");

	@Override
	public Binary getBinary()
	{
		return null;
	}

	@Override
	public String getDescription()
	{
		return "import from another sdf file";
	}

	@Override
	public String getName()
	{
		return "SDF Importer";
	}

	@Override
	public Messages getProcessMessages()
	{
		return null;
	}

	@Override
	public Property[] getProperties()
	{
		return new Property[] { sdfFile, idFeatureDataset, idFeatureSDF };
	}

	@Override
	public boolean[] build3D(DatasetFile dataset, String outFile) throws Exception
	{
		disableAutocorrect();

		CompoundProperty property = null;
		for (CompoundProperty prop : dataset.getIntegratedProperties())
		{
			if (prop.toString().equals(idFeatureDataset.getValue()))
			{
				property = prop;
				break;
			}
		}
		if (property == null)
			throw new ThreeDBuilderException(this, "property '" + idFeatureDataset.getValue()
					+ "' not available in dataset");
		List<String> vals = new ArrayList<String>();
		if (property.getType() != Type.NUMERIC)
			vals = ArrayUtil.toList(property.getStringValues(dataset));
		else if (property.isInteger(dataset))
			for (Double d : property.getDoubleValues(dataset))
				vals.add(String.valueOf(d.intValue()));
		else
			for (Double d : property.getDoubleValues(dataset))
				vals.add(String.valueOf(d));
		for (String v : vals)
			if (v == null || v.length() == 0)
				throw new ThreeDBuilderException(this, "property '" + idFeatureDataset.getValue()
						+ "' not available for all compounds in dataset");

		List<String> idsInSDF = new ArrayList<String>();
		ISimpleChemObjectReader reader = new ReaderFactory().createReader(new SDFReader(new FileInputStream(sdfFile
				.getValue())));
		IChemFile content = (IChemFile) reader.read((IChemObject) new ChemFile());
		for (IAtomContainer mol : ChemFileManipulator.getAllAtomContainers(content))
		{
			String v = (String) mol.getProperty(idFeatureSDF.getValue());
			idsInSDF.add(v);
		}
		reader.close();

		List<Integer> filterIndices = new ArrayList<Integer>();

		int count = 0;
		for (String v : vals)
		{
			int idx = idsInSDF.indexOf(v);
			if (idx == -1)
				throw new ThreeDBuilderException(this, "property value '" + v
						+ "' not found in import-SDF for proptery '" + idFeatureSDF.getValue() + "'");
			filterIndices.add(idx);
			System.out.println((count++) + ": " + v + " is at position " + idx + " in the sdf");
		}
		SDFUtil.filter(sdfFile.getValue().getAbsolutePath(), outFile, filterIndices, true);

		return null;
	}

}