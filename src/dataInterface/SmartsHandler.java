package dataInterface;

import java.util.List;

import data.DatasetFile;

public interface SmartsHandler
{
	public List<boolean[]> match(List<String> smarts, List<Integer> minNumMatches, DatasetFile dataset);

}
