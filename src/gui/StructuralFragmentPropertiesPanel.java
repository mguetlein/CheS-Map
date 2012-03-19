package gui;

import gui.property.PropertyPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JPanel;

import main.BinHandler;
import main.PropHandler;
import main.Settings;
import util.ImageLoader;
import util.SwingUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import data.fragments.MatchEngine;
import data.fragments.StructuralFragmentProperties;

public class StructuralFragmentPropertiesPanel extends JPanel
{
	private PropertyPanel propPanel;
	private JComponent babelPanel;

	public StructuralFragmentPropertiesPanel()
	{
		propPanel = new PropertyPanel(StructuralFragmentProperties.getProperties(), PropHandler.getProperties(),
				PropHandler.getPropertiesFile());
		babelPanel = BinHandler.getBinaryComponent(BinHandler.BABEL_BINARY);

		DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("p,fill:p:grow"));
		b.append(propPanel);
		b.append(babelPanel, 2);

		setLayout(new BorderLayout());
		add(b.getPanel(), BorderLayout.WEST);

		if (!BinHandler.BABEL_BINARY.isFound())
			StructuralFragmentProperties.setMatchEngine(MatchEngine.CDK);

		StructuralFragmentProperties.addMatchEngingePropertyChangeListenerToProperties(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				babelPanel.setVisible(StructuralFragmentProperties.getMatchEngine() == MatchEngine.OpenBabel);
				StructuralFragmentPropertiesPanel.this.revalidate();
				StructuralFragmentPropertiesPanel.this.repaint();
			}
		});
	}

	public void store()
	{
		propPanel.store();
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
									babelPanel.setVisible(StructuralFragmentProperties.getMatchEngine() == MatchEngine.OpenBabel);
								}
							}, Settings.TOP_LEVEL_FRAME);
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
			if (StructuralFragmentProperties.isSkipOmniFragments())
				skip = "skip omnipresent fragments, ";
			l.setText("Settings for fragments: min-frequency " + StructuralFragmentProperties.getMinFrequency() + ", "
					+ skip + "match with " + StructuralFragmentProperties.getMatchEngine());
		}
	}

}
