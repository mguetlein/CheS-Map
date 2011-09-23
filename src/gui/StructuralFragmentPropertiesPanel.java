package gui;

import gui.property.BooleanProperty;
import gui.property.IntegerProperty;
import gui.property.Property;
import gui.property.PropertyPanel;
import gui.property.SelectProperty;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;

import main.Settings;
import util.ImageLoader;
import util.SwingUtil;
import data.StructuralFragments.MatchEngine;

public class StructuralFragmentPropertiesPanel extends PropertyPanel
{
	static IntegerProperty minFreqProp = new IntegerProperty("Minimum frequency", "Minimum frequency", 1, 1, 1,
			Integer.MAX_VALUE);
	static BooleanProperty skipOmniProp = new BooleanProperty("Skip fragments that match all compounds", true, true);

	static SelectProperty matchEngine = new SelectProperty("Smarts matching software for smarts files",
			MatchEngine.values(), MatchEngine.OpenBabel, MatchEngine.OpenBabel);

	private static Property[] properties = new Property[] { minFreqProp, skipOmniProp, matchEngine };

	public StructuralFragmentPropertiesPanel()
	{
		super(properties, Settings.PROPS, Settings.PROPERTIES_FILE);

		if (!Settings.BABEL_BINARY.isFound())
			matchEngine.setValue(MatchEngine.CDK);
	}

	public int getMinFrequency()
	{
		return minFreqProp.getValue();
	}

	public boolean isSkipOmniFragments()
	{
		return skipOmniProp.getValue();
	}

	public MatchEngine getMatchEngine()
	{
		return (MatchEngine) matchEngine.getValue();
	}

	public JPanel getSummaryPanel()
	{
		return new SummaryPanel();
	}

	class SummaryPanel extends JPanel
	{
		LinkButton l;

		public SummaryPanel()
		{
			l = new LinkButton("");
			l.setForegroundFont(l.getFont().deriveFont(Font.PLAIN));
			l.setSelectedForegroundFont(l.getFont().deriveFont(Font.PLAIN));
			l.setSelectedForegroundColor(Color.BLUE);
			l.setIcon(ImageLoader.TOOL);
			l.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					SwingUtil.showInDialog(StructuralFragmentPropertiesPanel.this, "Settings for structural fragments");
				}
			});

			setLayout(new BorderLayout());
			add(l, BorderLayout.WEST);
			update();

			addPropertyChangeListenerToProperties(new PropertyChangeListener()
			{
				@Override
				public void propertyChange(PropertyChangeEvent evt)
				{
					update();
				}
			});
		}

		private void update()
		{
			String skip = "";
			if (isSkipOmniFragments())
				skip = "skip omnipresent fragments, ";
			l.setText("Settings for fragments: min-frequency " + getMinFrequency() + ", " + skip + "match with "
					+ getMatchEngine());
		}
	}

}
