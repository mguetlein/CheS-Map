package dataInterface;

import java.util.HashSet;

import util.ArrayUtil;

public abstract class AbstractPropertySet implements CompoundPropertySet
{
	private Type type;
	private HashSet<Type> types = new HashSet<Type>(ArrayUtil.toList(Type.values()));

	@Override
	public Type getType()
	{
		return type;
	}

	@Override
	public void setType(Type type)
	{
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

}
