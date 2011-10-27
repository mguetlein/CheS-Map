package dataInterface;

import main.Settings;

public enum SubstructureSmartsType
{
	MCS, MAX_FRAG;

	public String getName()
	{
		if (this == MCS)
			return Settings.text("align.mcs.short");
		else
			return Settings.text("align.max-frag.short");
	}
}
