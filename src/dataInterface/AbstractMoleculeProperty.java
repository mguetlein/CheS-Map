package dataInterface;

import java.util.HashSet;

import util.ArrayUtil;

public abstract class AbstractMoleculeProperty implements MoleculeProperty
{
	Type type;
	HashSet<Type> types = new HashSet<Type>(ArrayUtil.toList(Type.values()));
	Object[] domain;

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

	@Override
	public Object[] getNominalDomain()
	{
		return domain;
	}

	@Override
	public void setNominalDomain(Object domain[])
	{
		this.domain = domain;
	}

}
