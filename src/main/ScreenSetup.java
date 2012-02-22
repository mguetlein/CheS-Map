package main;

import java.awt.Dimension;
import java.awt.Window;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import util.ScreenUtil;

public class ScreenSetup
{
	Border wizardBorder;
	int screen;
	Dimension size;
	Dimension fullScreenSize;
	boolean antialiasOn;

	public static final ScreenSetup DEFAULT = new ScreenSetup(new EmptyBorder(0, 0, 0, 0), null, false);
	public static final ScreenSetup SCREENSHOT = new ScreenSetup(new EtchedBorder(), new Dimension(1200, 750), true);
	public static final ScreenSetup VIDEO = new ScreenSetup(new EmptyBorder(0, 0, 0, 0), new Dimension(1200, 750), true);

	public static ScreenSetup SETUP = DEFAULT;

	private ScreenSetup(Border wizardBorder, Dimension fixedSize, boolean antialiasOn)
	{
		this.wizardBorder = wizardBorder;
		this.size = fixedSize;
		this.fullScreenSize = fixedSize;
		this.antialiasOn = antialiasOn;
		this.screen = ScreenUtil.getLargestScreen();
	}

	public Border getWizardBorder()
	{
		return wizardBorder;
	}

	public void centerOnScreen(Window w)
	{
		ScreenUtil.centerOnScreen(w, screen);
	}

	public Dimension getSize()
	{
		if (size == null)
		{
			Dimension dim = ScreenUtil.getScreenSize(this.screen);
			return new Dimension(dim.width - 200, dim.height - 200);
		}
		else
			return size;
	}

	public Dimension getFullScreenSize()
	{
		if (fullScreenSize == null)
			return ScreenUtil.getScreenSize(this.screen);
		else
			return fullScreenSize;
	}

	public boolean isAntialiasOn()
	{
		return antialiasOn;
	}

	public int getScreen()
	{
		return screen;
	}

	public void setScreen(int s)
	{
		screen = s;
	}
}
