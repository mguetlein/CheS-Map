package dataInterface;

public interface MoleculeProperty
{
	public static enum Type
	{
		NUMERIC, NOMINAL
	}

	public Type getType();

	public void setType(Type type);

	public boolean isTypeAllowed(Type type);

	public void setTypeAllowed(Type type, boolean allowed);

	public Object[] getNominalDomain();

	public void setNominalDomain(Object domain[]);

}
