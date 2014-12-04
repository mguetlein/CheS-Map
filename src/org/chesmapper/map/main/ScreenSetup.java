package org.chesmapper.map.main;

import java.awt.Dimension;
import java.awt.Window;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import org.mg.javalib.util.ScreenUtil;

public class ScreenSetup
{
	Border wizardBorder;
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
			new Dimension(1200, 750), true, 18, true);
	public static final ScreenSetup VIDEO = new ScreenSetup(new EmptyBorder(0, 0, 0, 0), new Dimension(1280, 720),
			new Dimension(1280, 720), false, 12, false);
	public static final ScreenSetup SMALL_SCREEN = new ScreenSetup(new EmptyBorder(0, 0, 0, 0),
			new Dimension(824, 568), new Dimension(924, 668), false, 12, false);
	public static final ScreenSetup SXGA_PLUS = new ScreenSetup(new EmptyBorder(0, 0, 0, 0), new Dimension(1400, 1050),
			new Dimension(1024, 768), false, 15, false);

	public static ScreenSetup INSTANCE = null;

	private ScreenSetup(Border wizardBorder, Dimension viewerFixedSize, Dimension wizardFixedSize, boolean antialiasOn,
			int fontSize, boolean wizardUndecorated)
	{
		this.wizardBorder = wizardBorder;
		this.viewerSize = viewerFixedSize;
		this.fullScreenSize = viewerFixedSize;
		this.wizardSize = wizardFixedSize;
		this.antialiasOn = antialiasOn;
		this.fontSize = fontSize;
		this.wizardUndecorated = wizardUndecorated;
	}

	public Border getWizardBorder()
	{
		return wizardBorder;
	}

	public void centerOnScreen(Window w)
	{
		ScreenUtil.centerOnScreen(w, Settings.TOP_LEVEL_FRAME_SCREEN);
	}

	public void setViewerSize(Dimension viewerSize)
	{
		this.viewerSize = viewerSize;
	}

	public Dimension getViewerSize()
	{
		if (viewerSize == null)
		{
			Dimension dim = ScreenUtil.getScreenSize(Settings.TOP_LEVEL_FRAME_SCREEN);
			return new Dimension(dim.width - 200, dim.height - 200);
		}
		else
			return viewerSize;
	}

	public void setWizardSize(Dimension wizardSize)
	{
		this.wizardSize = wizardSize;
	}

	public Dimension getWizardSize()
	{
		if (wizardSize == null)
		{
			Dimension d = new Dimension(1024, 768);
			Dimension full = ScreenSetup.INSTANCE.getFullScreenSize();
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
			return ScreenUtil.getScreenSize(Settings.TOP_LEVEL_FRAME_SCREEN);
		else
			return fullScreenSize;
	}

	public void setFullScreenSize(Dimension fullScreenSize)
	{
		this.fullScreenSize = fullScreenSize;
	}

	public boolean isAntialiasOn()
	{
		return antialiasOn;
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

	public boolean isFontSizeLarge()
	{
		return fontSize > 14;
	}

	public void setFontSize(int fontSize)
	{
		this.fontSize = fontSize;
	}

}
