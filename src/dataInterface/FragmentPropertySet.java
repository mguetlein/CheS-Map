package dataInterface;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import data.fragments.StructuralFragmentProperties;

public abstract class FragmentPropertySet implements CompoundPropertySet
{
	public FragmentPropertySet()
	{
		StructuralFragmentProperties.addPropertyChangeListenerToProperties(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				updateFragments();

			}
		});
	}

	protected abstract void updateFragments();
}
