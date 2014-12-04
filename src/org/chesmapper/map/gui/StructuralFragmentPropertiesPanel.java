package org.chesmapper.map.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.chesmapper.map.data.fragments.FragmentProperties;
import org.chesmapper.map.data.fragments.MatchEngine;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.PropHandler;
import org.chesmapper.map.main.Settings;
import org.mg.javalib.gui.LinkButton;
import org.mg.javalib.gui.property.PropertyPanel;
import org.mg.javalib.util.ImageLoader;
import org.mg.javalib.util.SwingUtil;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class StructuralFragmentPropertiesPanel extends JPanel
{
	private PropertyPanel propPanel;
	private JComponent babelPanel;

	public StructuralFragmentPropertiesPanel(final CheSMapperWizard wizard)
	{
		propPanel = new PropertyPanel(FragmentProperties.getProperties(), PropHandler.getProperties(),
				PropHandler.getPropertiesFile());
		babelPanel = BinHandler.getBinaryComponent(BinHandler.BABEL_BINARY, (Window) getTopLevelAncestor());

		DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("p,fill:p:grow"));
		b.append(propPanel);
		b.append(babelPanel, 2);

		setLayout(new BorderLayout());
		add(b.getPanel(), BorderLayout.WEST);

		if (!BinHandler.BABEL_BINARY.isFound())
			FragmentProperties.setMatchEngine(MatchEngine.CDK);

		FragmentProperties.addMatchEngingePropertyChangeListenerToProperties(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (wizard.isClosed())
					return;
				babelPanel.setVisible(FragmentProperties.getMatchEngine() == MatchEngine.OpenBabel);
				StructuralFragmentPropertiesPanel.this.revalidate();
				StructuralFragmentPropertiesPanel.this.repaint();
			}
		});
	}

	public void store()
	{
		propPanel.store();
	}

	public JPanel getSummaryPanel(Window owner)
	{
		return new SummaryPanel(owner);
	}

	public void showDialog(Window owner)
	{
		babelPanel.setVisible(true);
		SwingUtil.showInDialog(StructuralFragmentPropertiesPanel.this, "Settings for structural fragments", null,
				new Runnable()
				{
					@Override
					public void run()
					{
						babelPanel.setVisible(FragmentProperties.getMatchEngine() == MatchEngine.OpenBabel);
					}
				}, Settings.TOP_LEVEL_FRAME);
		if (owner != null)
			owner.setVisible(true);
	}

	class SummaryPanel extends JPanel
	{
		LinkButton l;

		public SummaryPanel(final Window owner)
		{
			l = new LinkButton("");
			l.setForegroundFont(l.getFont().deriveFont(Font.PLAIN));
			l.setSelectedForegroundFont(l.getFont().deriveFont(Font.PLAIN));
			l.setSelectedForegroundColor(Color.BLUE);
			l.setIcon(ImageLoader.getImage(ImageLoader.Image.tool));
			l.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							showDialog(owner);
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
			if (FragmentProperties.isSkipOmniFragments())
				skip = "skip omnipresent fragments, ";
			l.setText("Settings for fragments: min-frequency " + FragmentProperties.getMinFrequency() + ", " + skip
					+ "match with " + FragmentProperties.getMatchEngine());
		}
	}

}
