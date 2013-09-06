package data;

import java.util.List;

import main.BinHandler;
import main.Settings;
import babel.OBSmartsMatcher;
import dataInterface.SmartsHandler;

public class OpenBabelSmartsHandler implements SmartsHandler
{
	OBSmartsMatcher obSmartsMatcher;

	@Override
	public List<boolean[]> match(List<String> smarts, List<Integer> minNumMatches, DatasetFile dataset)
	{
		if (obSmartsMatcher == null)
			obSmartsMatcher = new OBSmartsMatcher(BinHandler.BABEL_BINARY.getLocation(),
					Settings.MODIFIED_BABEL_DATA_DIR, Settings.LOGGER);
		String file;
		if (dataset.getLocalPath() != null && dataset.getLocalPath().endsWith(".smi"))
			file = dataset.getLocalPath();
		else
			file = dataset.getSDFPath(false);
		return obSmartsMatcher.match(smarts, minNumMatches, file, dataset.numCompounds());
	}
}
