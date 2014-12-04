package org.chesmapper.map.gui;

import java.awt.Image;
import java.net.URL;
import java.net.URLEncoder;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class SmartsViewDialog extends JDialog
{
	public static boolean asked = false;

	public static void show(JFrame owner, String smarts, int maxWidth, int maxHeight)
	{
		if (!asked)
		{
			int ret = JOptionPane
					.showConfirmDialog(
							owner,
							"This will visualize the SMARTS string by accessing http://www.smartsview.de.\nThe only data sent to this service is the SMARTS fragment. Continue?",
							"Grant internet access?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (ret == JOptionPane.YES_OPTION)
				asked = true;
			else
				return;
		}
		SmartsViewDialog d = new SmartsViewDialog(owner, smarts, maxWidth, maxHeight);
		d.setVisible(true);
	}

	private SmartsViewDialog(JFrame owner, String smarts, int maxWidth, int maxHeight)
	{
		super(owner, smarts, true);
		JPanel p = new JPanel();
		try
		{
			String url = "http://www.smartsview.de/smartsview/auto/png/1/both/" + URLEncoder.encode(smarts, "UTF-8");
			ImageIcon icon = new ImageIcon(new URL(url));
			if (icon.getIconWidth() > maxWidth || icon.getIconHeight() > maxHeight)
			{
				Image img = icon.getImage();
				double sx = maxWidth / (double) icon.getIconWidth();
				double sy = maxHeight / (double) icon.getIconHeight();
				double scale = Math.min(Math.min(sx, sy), 1);
				int scaledWidth = (int) (icon.getIconWidth() * scale);
				int scaledHeight = (int) (icon.getIconHeight() * scale);
				Image newimg = img.getScaledInstance(scaledWidth, scaledHeight, java.awt.Image.SCALE_SMOOTH);
				icon = new ImageIcon(newimg);
			}
			if (icon.getIconWidth() <= 0)
				throw new Exception("Could not load icon from " + url);
			p.add(new JLabel(icon));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			p.add(new JLabel("ERROR: " + e.getMessage()));
			p.setBorder(new EmptyBorder(10, 10, 10, 10));
		}
		add(p);
		pack();
		setLocationRelativeTo(owner);
	}

	public static void main(String[] args)
	{
		show(null, "[#6][#8]", 300, 400);
		System.exit(1);
	}
}
