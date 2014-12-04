package org.chesmapper.map.main;

import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.chesmapper.map.property.OBDescriptorFactory;
import org.mg.javalib.babel.OBWrapper;
import org.mg.javalib.gui.LinkButton;
import org.mg.javalib.gui.binloc.Binary;
import org.mg.javalib.gui.binloc.BinaryLocator;
import org.mg.javalib.gui.binloc.BinaryLocatorDialog;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.ImageLoader;
import org.mg.javalib.util.OSUtil;

public class BinHandler
{
	public static Binary BABEL_BINARY = new Binary("babel", "CM_BABEL_PATH", Settings.OPENBABEL_STRING);
	public static Binary RSCRIPT_BINARY = new Binary("Rscript", "CM_RSCRIPT_PATH", Settings.R_STRING);

	public static class FminerBinary extends Binary
	{
		public FminerBinary()
		{
			super("fminer", "CM_FMINER_PATH", Settings.R_STRING);
		}

		public String getBBRCLib()
		{
			if (!isFound())
				return null;
			String bbrcLib = FileUtil.getParent(FileUtil.getParent(getLocation())) + "/libbbrc/libbbrc.so";
			if (!new File(bbrcLib).exists())
				bbrcLib = FileUtil.getParent(FileUtil.getParent(getLocation())) + "/libbbrc/bbrc.so";
			if (!new File(bbrcLib).exists())
			{
				Settings.LOGGER.error("BBRC LIB not found, should be at " + bbrcLib + " , libbbrc.so not found either");
				return null;
			}
			else
				return bbrcLib;
		}
	}

	public static FminerBinary FMINER_BINARY = new FminerBinary();

	private static OBWrapper OB_WRAPPER;

	private static String babelVersion = null;
	private static List<Binary> bins;

	public static void init()
	{
		if (bins != null)
			throw new IllegalStateException("init only once");

		bins = new ArrayList<Binary>();
		bins.add(BABEL_BINARY);
		BABEL_BINARY.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				babelVersion = null;
			}
		});
		bins.add(RSCRIPT_BINARY);
		for (Binary binary : bins)
		{
			String path = PropHandler.get("bin-path-" + binary.getCommand());
			if (path != null)
				binary.setLocation(path);
		}
		locateBinarys();
		for (Binary binary : bins)
		{
			if (binary.isFound())
				Settings.LOGGER.info("External program " + binary.getCommand() + " found at " + binary.getLocation());
			else
				Settings.LOGGER.warn("External program " + binary.getCommand() + " not found");
		}
		if (BABEL_BINARY.isFound())
		{
			new Thread(new Runnable()
			{
				public void run()
				{
					OBDescriptorFactory.getDescriptorIDs(false);
				}
			}).start();
		}
	}

	public static OBWrapper getOBWrapper()
	{
		if (OB_WRAPPER == null)
			OB_WRAPPER = new OBWrapper(BABEL_BINARY.getLocation(), BABEL_BINARY.getSisterCommandLocation("obabel"),
					Settings.LOGGER);
		return OB_WRAPPER;
	}

	public static String getOpenBabelVersion()
	{
		if (!BABEL_BINARY.isFound())
			throw new IllegalStateException();
		if (babelVersion == null)
			babelVersion = getOBWrapper().getVersion();
		return babelVersion;
	}

	public static String getOBFileModified(String destinationFilename)
	{
		return Settings.MODIFIED_BABEL_DATA_DIR + File.separator + destinationFilename;
	}

	public static String getOBFileOrig(String s)
	{
		if (!BABEL_BINARY.isFound())
			throw new IllegalStateException();
		String p = FileUtil.getParent(BABEL_BINARY.getLocation());
		String babelDatadir;
		if (OSUtil.isWindows())
			babelDatadir = System.getenv("BABEL_DATADIR");
		else
			babelDatadir = System.getenv().get("BABEL_DATADIR");
		if (babelDatadir != null)
		{
			String f = System.getenv("BABEL_DATADIR") + File.separator + s;
			if (new File(f).exists())
				return f;
		}
		if (OSUtil.isWindows())
		{
			String f = p + "\\data\\" + s;
			if (new File(f).exists())
				return f;
			throw new Error("not found: " + f);
		}
		else
		{
			// default dir
			String f = "/usr/local/share/openbabel/" + getOpenBabelVersion() + "/" + s;
			if (new File(f).exists())
				return f;
			// hack
			while (p.length() > 1)
			{
				f = p + "/share/openbabel/" + getOpenBabelVersion() + "/" + s;
				if (new File(f).exists())
					return f;
				f = p + "/install/share/openbabel/" + getOpenBabelVersion() + "/" + s;
				if (new File(f).exists())
					return f;
				p = FileUtil.getParent(p);
			}
			throw new Error("not found: " + s);
		}
	}

	public static void locateBinarys()
	{
		if (bins == null)
			throw new IllegalStateException("init first");
		BinaryLocator.locate(bins);
	}

	public static void showBinaryDialog(Binary select, Window owner)
	{
		if (bins == null)
			throw new IllegalStateException("init first");
		locateBinarys();
		new BinaryLocatorDialog(owner, "External Programs", Settings.TITLE, bins, select);
		for (Binary binary : bins)
			if (binary.getLocation() != null)
				PropHandler.put("bin-path-" + binary.getCommand(), binary.getLocation());
			else
				PropHandler.remove("bin-path-" + binary.getCommand());
		PropHandler.storeProperties();
		if (owner != null)
			owner.setVisible(true);
	}

	public static JComponent getBinaryComponent(final Binary bin, final Window owner)
	{
		final LinkButton l = new LinkButton("Configure external program: " + bin.getDescription());
		l.setForegroundFont(l.getFont().deriveFont(Font.PLAIN));
		l.setSelectedForegroundFont(l.getFont().deriveFont(Font.PLAIN));
		l.setSelectedForegroundColor(Color.BLUE);
		bin.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (bin.isFound())
					l.setIcon(ImageLoader.getImage(ImageLoader.Image.tool));
			}
		});
		if (!bin.isFound())
			l.setIcon(ImageLoader.getImage(ImageLoader.Image.error));
		else
			l.setIcon(ImageLoader.getImage(ImageLoader.Image.tool));
		l.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				showBinaryDialog(bin, owner);
			}
		});
		return l;
	}
}
