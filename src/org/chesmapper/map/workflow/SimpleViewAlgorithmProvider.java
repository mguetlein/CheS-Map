package org.chesmapper.map.workflow;

import org.chesmapper.map.alg.Algorithm;

public interface SimpleViewAlgorithmProvider extends AlgorithmProvider
{
	/**
	 * is the yes choice default in the simple view (currently: clustering disabled by default) 
	 */
	public boolean isYesDefault();

	/**
	 * get algorithm for yes option
	 */
	public Algorithm getYesAlgorithm();

	/**
	 * get algorithm for no-option (often this is just a algorithm that does nothing, e.g. no-clusterer)
	 */
	public Algorithm getNoAlgorithm();

	/**
	 * stores the simple view selection to props (accordingly, the yes/no algorithm will be used)
	 */
	public void storeSimpleSelectionToProps(boolean simpleSelected, boolean yesSelected);

	/**
	 * is simple view selected in global props?
	 */
	public boolean isSimpleSelectedFromProps();

	/**
	 * is yes selected in global props?
	 */
	public boolean isYesSelectedFromProps();
}
