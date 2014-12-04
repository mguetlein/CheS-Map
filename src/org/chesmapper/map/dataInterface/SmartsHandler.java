package org.chesmapper.map.dataInterface;

import java.util.List;

import org.chesmapper.map.data.DatasetFile;

public interface SmartsHandler
{
	public List<boolean[]> match(List<String> smarts, List<Integer> minNumMatches, DatasetFile dataset);

}
