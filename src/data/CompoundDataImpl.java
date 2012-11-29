package data;

import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.vecmath.Vector3f;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IMolecule;

import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;

public class CompoundDataImpl implements CompoundData
{
	private Vector3f position;
	private int index;
	private HashMap<MoleculeProperty, String> stringValues = new HashMap<MoleculeProperty, String>();
	private HashMap<MoleculeProperty, Double> doubleValues = new HashMap<MoleculeProperty, Double>();
	private HashMap<MoleculeProperty, Double> normalizedValues = new HashMap<MoleculeProperty, Double>();
	private String smiles;
	private IMolecule iMolecule;

	public CompoundDataImpl(String smiles, IMolecule m)
	{
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

	public int getIndex()
	{
		return index;
	}

	public void setIndex(int index)
	{
		this.index = index;
	}

	public void setDoubleValue(MoleculeProperty p, Double v)
	{
		doubleValues.put(p, v);
	}

	public void setStringValue(MoleculeProperty p, String v)
	{
		stringValues.put(p, v);
	}

	public void setNormalizedValue(MoleculeProperty p, Double v)
	{
		normalizedValues.put(p, v);
	}

	public Double getDoubleValue(MoleculeProperty p)
	{
		if (p.getType() != Type.NUMERIC)
			throw new IllegalStateException();
		return doubleValues.get(p);
	}

	public String getStringValue(MoleculeProperty p)
	{
		if (p.getType() == Type.NUMERIC)
			throw new IllegalStateException();
		return stringValues.get(p);
	}

	public Double getNormalizedValue(MoleculeProperty p)
	{
		if (p.getType() != Type.NUMERIC)
			throw new IllegalStateException();
		return normalizedValues.get(p);
	}

	@Override
	public String getSmiles()
	{
		return smiles;
	}

	ImageIcon iconBlack;
	ImageIcon iconWhite;

	@Override
	public ImageIcon getIcon(boolean black)
	{
		if ((black && iconBlack == null) || (!black && iconWhite == null))
		{
			try
			{
				if (black)
					iconBlack = CDKCompoundIcon.createIcon(iMolecule, black);
				else
					iconWhite = CDKCompoundIcon.createIcon(iMolecule, black);
			}
			catch (CDKException e)
			{
				System.err.println("cannot created 2d image");
				e.printStackTrace();
			}
		}
		return black ? iconBlack : iconWhite;
	}
}
