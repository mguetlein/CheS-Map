package org.chesmapper.map.data;

import java.util.List;

import org.chesmapper.map.dataInterface.SmartsHandler;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.Settings;
import org.mg.javalib.babel.OBSmartsMatcher;

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
			file = dataset.getSDF();
		return obSmartsMatcher.match(smarts, minNumMatches, file, dataset.numCompounds());
	}
}
