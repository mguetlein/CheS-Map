package alg;

import gui.binloc.Binary;
import gui.property.Property;

public interface Algorithm
{
	public Property[] getProperties();

	public void setProperties(Property[] properties);

	public String getName();

	public String getDescription();

	public Binary getBinary();
}
