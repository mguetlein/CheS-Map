package org.chesmapper.map.util;

import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import org.chesmapper.map.main.ScreenSetup;

public class WizardComponentFactory
{
	/**
	 * dynamic border (only visible if vertical scrollbar is visible)
	 * 
	 * @param comp
	 * @return
	 */
	public static JScrollPane getVerticalScrollPane(final JComponent comp)
	{
		final JScrollPane scroll = new JScrollPane(comp);
		if (!ScreenSetup.INSTANCE.isWizardSpaceSmall())
			scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(new EmptyBorder(1, 1, 1, 0));
		scroll.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener()
		{

			@Override
			public void adjustmentValueChanged(AdjustmentEvent e)
			{
				if (scroll.getVerticalScrollBar().isVisible())
				{
					scroll.setViewportBorder(new MatteBorder(1, 1, 1, 0, comp.getBackground().darker().darker()));
				}
				else
				{
					scroll.setViewportBorder(new EmptyBorder(1, 1, 1, 0));
				}
				scroll.repaint();
			}
		});
		return scroll;
	}
}
