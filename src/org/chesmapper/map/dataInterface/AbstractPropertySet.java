package org.chesmapper.map.dataInterface;

import java.util.HashSet;

import org.chesmapper.map.data.DatasetFile;
import org.mg.javalib.util.ArrayUtil;

public abstract class AbstractPropertySet implements CompoundPropertySet
{
	private Type type;
	private HashSet<Type> types = new HashSet<Type>(ArrayUtil.toList(Type.values()));
	private boolean smiles;

	@Override
	public Type getType()
	{
		return type;
	}

	@Override
	public void setType(Type type)
	{
		if (type != null && !isTypeAllowed(type))
			throw new IllegalArgumentException("type " + type + " not allowed for " + this);
		this.type = type;
	}

	@Override
	public boolean isTypeAllowed(Type type)
	{
		return types.contains(type);
	}

	@Override
	public void setTypeAllowed(Type type, boolean allowed)
	{
		if (allowed)
			types.add(type);
		else if (types.contains(type))
			types.remove(type);
	}

	public void setSmiles(boolean smiles)
	{
		this.smiles = smiles;
	}

	public boolean isSmiles()
	{
		return smiles;
	}

	@Override
	public boolean isHiddenFromGUI()
	{
		return false;
	}

	@Override
	public boolean isSizeDynamicHigh(DatasetFile dataset)
	{
		throw new IllegalStateException();
	}
}
