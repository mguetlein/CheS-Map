package data;

import gui.MultiImageIcon.Layout;

import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.vecmath.Vector3f;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import dataInterface.CompoundData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;

public class CompoundDataImpl implements CompoundData
{
	private Vector3f position;
	private int origIndex = -1;
	private HashMap<CompoundProperty, String> stringValues = new HashMap<CompoundProperty, String>();
	private HashMap<CompoundProperty, Double> doubleValues = new HashMap<CompoundProperty, Double>();
	private HashMap<CompoundProperty, Double> normalizedValuesCompleteDataset = new HashMap<CompoundProperty, Double>();
	private String smiles;
	private IAtomContainer iMolecule;

	public CompoundDataImpl(String smiles, IAtomContainer m)
	{
		if (smiles == null || smiles.toString().length() == 0)
			throw new IllegalArgumentException();
		this.smiles = smiles;
		this.iMolecule = m;
	}

	public Vector3f getPosition()
	{
		return position;
	}

	public void setPosition(Vector3f position)
	{
		this.position = position;
	}

	public int getOrigIndex()
	{
		return origIndex;
	}

	public void setOrigIndex(int origIndex)
	{
		this.origIndex = origIndex;
	}

	public void setDoubleValue(CompoundProperty p, Double v)
	{
		doubleValues.put(p, v);
	}

	public void setStringValue(CompoundProperty p, String v)
	{
		stringValues.put(p, v);
	}

	public void setNormalizedValueCompleteDataset(CompoundProperty p, Double v)
	{
		normalizedValuesCompleteDataset.put(p, v);
	}

	@Override
	public Double getDoubleValue(CompoundProperty p)
	{
		if (p.getType() != Type.NUMERIC)
			throw new IllegalStateException();
		return doubleValues.get(p);
	}

	@Override
	public String getStringValue(CompoundProperty p)
	{
		if (p.getType() == Type.NUMERIC)
			throw new IllegalStateException(p + " is numeric!");
		return stringValues.get(p);
	}

	@Override
	public Double getNormalizedValueCompleteDataset(CompoundProperty p)
	{
		if (p.getType() != Type.NUMERIC)
			throw new IllegalStateException();
		return normalizedValuesCompleteDataset.get(p);
	}

	@Override
	public String getFormattedValue(CompoundProperty p)
	{
		if (p.getType() == Type.NUMERIC)
			return p.getFormattedValueInMappedDataset(getDoubleValue(p));
		else
			return p.getFormattedValueInMappedDataset(getStringValue(p));
	}

	@Override
	public String getSmiles()
	{
		return smiles;
	}

	HashMap<String, ImageIcon> icons = new HashMap<String, ImageIcon>();

	@Override
	public ImageIcon getIcon(boolean black, int width, int height, boolean translucent)
	{
		String key = black + "#" + width + "#" + height + "#" + translucent;
		if (!icons.containsKey(key))
		{
			try
			{
				icons.put(key,
						CDKCompoundIcon.createIcon(iMolecule, black, width, height, Layout.vertical, translucent));
			}
			catch (CDKException e)
			{
				System.err.println("cannot created 2d image");
				e.printStackTrace();
			}
		}
		return icons.get(key);
	}
}
