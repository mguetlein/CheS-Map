package org.chesmapper.map.dataInterface;

import org.chesmapper.map.main.Settings;

public enum SubstructureSmartsType
{
	MCS, MAX_FRAG, MANUAL;

	public String getName()
	{
		if (this == MCS)
			return Settings.text("align.mcs.short");
		else if (this == MANUAL)
			return Settings.text("align.manual.short");
		else
			return Settings.text("align.max-frag.short");
	}
}
