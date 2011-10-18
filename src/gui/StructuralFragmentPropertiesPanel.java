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

import javax.swing.JComponent;
import javax.swing.JPanel;

import main.Settings;
import util.ImageLoader;
import util.SwingUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import data.StructuralFragments.MatchEngine;

public class StructuralFragmentPropertiesPanel extends JPanel
{
	static IntegerProperty minFreqProp = new IntegerProperty("Minimum frequency", "Minimum frequency", 1, 1,
			Integer.MAX_VALUE);
	static BooleanProperty skipOmniProp = new BooleanProperty("Skip fragments that match all compounds", true);
	static SelectProperty matchEngine = new SelectProperty("Smarts matching software for smarts files",
			MatchEngine.values(), MatchEngine.OpenBabel);

	private static Property[] properties = new Property[] { minFreqProp, skipOmniProp, matchEngine };

	private PropertyPanel propPanel;
	private JComponent babelPanel;

	public StructuralFragmentPropertiesPanel()
	{
		propPanel = new PropertyPanel(properties, Settings.PROPS, Settings.PROPERTIES_FILE);
		babelPanel = Settings.getBinaryComponent(Settings.BABEL_BINARY);

		DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("p,fill:p:grow"));
		b.append(propPanel);
		b.append(babelPanel, 2);

		setLayout(new BorderLayout());
		add(b.getPanel(), BorderLayout.WEST);

		if (!Settings.BABEL_BINARY.isFound())
			matchEngine.setValue(MatchEngine.CDK);

		matchEngine.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				babelPanel.setVisible(getMatchEngine() == MatchEngine.OpenBabel);
				StructuralFragmentPropertiesPanel.this.revalidate();
				StructuralFragmentPropertiesPanel.this.repaint();
			}
		});
	}

	public void addPropertyChangeListenerToProperties(PropertyChangeListener l)
	{
		propPanel.addPropertyChangeListenerToProperties(l);
	}

	public void store()
	{
		propPanel.store();
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
					babelPanel.setVisible(true);
					SwingUtil.showInDialog(StructuralFragmentPropertiesPanel.this, "Settings for structural fragments",
							null, new Runnable()
							{
								@Override
								public void run()
								{
									babelPanel.setVisible(getMatchEngine() == MatchEngine.OpenBabel);
								}
							});
				}
			});

			setLayout(new BorderLayout());
			add(l, BorderLayout.WEST);
			update();

			propPanel.addPropertyChangeListenerToProperties(new PropertyChangeListener()
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
