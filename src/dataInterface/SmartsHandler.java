package dataInterface;

import data.DatasetFile;
import data.StructuralAlerts;

public interface SmartsHandler
{
	//	public boolean isValidSmarts(String smarts);

	public String[] match(StructuralAlerts.Alert alert, DatasetFile dataset);

}
