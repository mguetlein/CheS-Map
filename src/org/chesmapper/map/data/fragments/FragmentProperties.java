package org.chesmapper.map.data.fragments;

import java.beans.PropertyChangeListener;

import org.chesmapper.map.main.BinHandler;
import org.mg.javalib.gui.property.BooleanProperty;
import org.mg.javalib.gui.property.IntegerProperty;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.gui.property.SelectProperty;

public class FragmentProperties
{
	private IntegerProperty minFreqProp = new IntegerProperty("Minimum frequency", "Minimum frequency", 10, 1,
			Integer.MAX_VALUE);
	private BooleanProperty skipOmniProp = new BooleanProperty("Skip fragments that match all compounds", true);
	private SelectProperty matchEngine = new SelectProperty("Smarts matching software for smarts files",
			MatchEngine.values(), BinHandler.BABEL_BINARY.isFound() ? MatchEngine.OpenBabel : MatchEngine.CDK);

	Property[] fragmentProps = new Property[] { minFreqProp, skipOmniProp, matchEngine };

	private FragmentProperties()
	{
	}

	private static FragmentProperties INSTANCE = new FragmentProperties();

	public static Property[] getProperties()
	{
		return INSTANCE.fragmentProps;
	}

	public static void addPropertyChangeListenerToProperties(PropertyChangeListener l, boolean ignoreMatchEngine)
	{
		for (Property p : INSTANCE.fragmentProps)
			if (!ignoreMatchEngine || p != INSTANCE.matchEngine)
				p.addPropertyChangeListener(l);
	}

	public static void addMatchEngingePropertyChangeListenerToProperties(PropertyChangeListener l)
	{
		INSTANCE.matchEngine.addPropertyChangeListener(l);
	}

	public static void setMinFrequency(int i)
	{
		INSTANCE.minFreqProp.setValue(i);
	}

	public static int getMinFrequency()
	{
		return INSTANCE.minFreqProp.getValue();
	}

	public static boolean isSkipOmniFragments()
	{
		return INSTANCE.skipOmniProp.getValue();
	}

	public static void setSkipOmniFragments(boolean b)
	{
		INSTANCE.skipOmniProp.setValue(b);
	}

	public static void setMatchEngine(MatchEngine e)
	{
		INSTANCE.matchEngine.setValue(e);
	}

	public static MatchEngine getMatchEngine()
	{
		return (MatchEngine) INSTANCE.matchEngine.getValue();
	}

	public static void resetDefaults()
	{
		for (Property p : INSTANCE.fragmentProps)
			p.setValue(p.getDefaultValue());
	}

}
