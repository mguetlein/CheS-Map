package data.fragments;

import gui.property.BooleanProperty;
import gui.property.IntegerProperty;
import gui.property.Property;
import gui.property.SelectProperty;

import java.beans.PropertyChangeListener;

public class StructuralFragmentProperties
{
	private IntegerProperty minFreqProp = new IntegerProperty("Minimum frequency", "Minimum frequency", 10, 1,
			Integer.MAX_VALUE);
	private BooleanProperty skipOmniProp = new BooleanProperty("Skip fragments that match all compounds", true);
	private SelectProperty matchEngine = new SelectProperty("Smarts matching software for smarts files",
			MatchEngine.values(), MatchEngine.OpenBabel);

	Property[] fragmentProps = new Property[] { minFreqProp, skipOmniProp, matchEngine };

	private StructuralFragmentProperties()
	{
	}

	private static StructuralFragmentProperties INSTANCE = new StructuralFragmentProperties();

	public static Property[] getProperties()
	{
		return INSTANCE.fragmentProps;
	}

	public static void addPropertyChangeListenerToProperties(PropertyChangeListener l)
	{
		for (Property p : INSTANCE.fragmentProps)
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
