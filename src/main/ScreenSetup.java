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
	Dimension viewerSize;
	Dimension wizardSize;
	Dimension fullScreenSize;
	boolean antialiasOn;
	int fontSize;
	boolean wizardUndecorated;

	public static final ScreenSetup DEFAULT = new ScreenSetup(new EmptyBorder(0, 0, 0, 0), null, null, false, 12, false);
	//	public static final ScreenSetup SCREENSHOT = new ScreenSetup(new EtchedBorder(), new Dimension(1200, 750),
	//			new Dimension(600, 375), true, 10, true);
	public static final ScreenSetup SCREENSHOT = new ScreenSetup(new EtchedBorder(), new Dimension(1200, 750),
			new Dimension(1200, 750), true, 20, true);
	public static final ScreenSetup VIDEO = new ScreenSetup(new EmptyBorder(0, 0, 0, 0), new Dimension(1024, 768),
			new Dimension(1024, 768), true, 12, false);

	public static ScreenSetup SETUP = DEFAULT;

	private ScreenSetup(Border wizardBorder, Dimension viewerFixedSize, Dimension wizardFixedSize, boolean antialiasOn,
			int fontSize, boolean wizardUndecorated)
	{
		this.wizardBorder = wizardBorder;
		this.viewerSize = viewerFixedSize;
		this.fullScreenSize = viewerFixedSize;
		this.wizardSize = wizardFixedSize;
		this.antialiasOn = antialiasOn;
		this.screen = ScreenUtil.getLargestScreen();
		this.fontSize = fontSize;
		this.wizardUndecorated = wizardUndecorated;
	}

	public Border getWizardBorder()
	{
		return wizardBorder;
	}

	public void centerOnScreen(Window w)
	{
		ScreenUtil.centerOnScreen(w, screen);
	}

	public Dimension getViewerSize()
	{
		if (viewerSize == null)
		{
			Dimension dim = ScreenUtil.getScreenSize(this.screen);
			return new Dimension(dim.width - 200, dim.height - 200);
		}
		else
			return viewerSize;
	}

	public Dimension getWizardSize()
	{
		if (wizardSize == null)
		{
			Dimension d = new Dimension(1024, 768);
			Dimension full = ScreenSetup.SETUP.getFullScreenSize();
			d.width = Math.min(full.width - 100, d.width);
			d.height = Math.min(full.height - 100, d.height);
			return d;
		}
		else
			return wizardSize;
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

	public int getFontSize()
	{
		return fontSize;
	}

	public boolean isWizardUndecorated()
	{
		return wizardUndecorated;
	}

	public boolean isWizardSpaceSmall()
	{
		return getWizardSize().getHeight() <= 500 || fontSize > 12;
	}
}
