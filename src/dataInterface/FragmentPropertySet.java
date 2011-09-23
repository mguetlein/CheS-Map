package dataInterface;

import data.StructuralFragments.MatchEngine;

public abstract class FragmentPropertySet implements MoleculePropertySet
{
	protected MatchEngine matchEngine;
	protected int minFrequency = -1;
	protected boolean skipOmnipresent = false;

	public void setMinFrequency(int minFrequency)
	{
		if (this.minFrequency != minFrequency)
		{
			this.minFrequency = minFrequency;
			updateFragments();
		}
	}

	public void setSkipOmniFragments(boolean skipOmniFragments)
	{
		if (this.skipOmnipresent != skipOmniFragments)
		{
			this.skipOmnipresent = skipOmniFragments;
			updateFragments();
		}
	}

	public void setMatchEngine(MatchEngine matchEngine)
	{
		if (this.matchEngine != matchEngine)
		{
			this.matchEngine = matchEngine;
			updateFragments();
		}
	}

	protected abstract void updateFragments();

}
