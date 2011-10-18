package alg;

import gui.binloc.Binary;
import gui.property.Property;

public interface Algorithm
{
	public Property[] getProperties();

	public String getName();

	public String getDescription();

	public Binary getBinary();

	public String getWarning();
}
