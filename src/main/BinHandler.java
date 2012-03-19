package main;

import gui.LinkButton;
import gui.binloc.Binary;
import gui.binloc.BinaryLocator;
import gui.binloc.BinaryLocatorDialog;
import io.ExternalTool;

import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;

import util.FileUtil;
import util.ImageLoader;
import util.OSUtil;

public class BinHandler
{
	public static Binary BABEL_BINARY = new Binary("babel", "CM_BABEL_PATH", Settings.OPENBABEL_STRING);
	public static Binary RSCRIPT_BINARY = new Binary("Rscript", "CM_RSCRIPT_PATH", Settings.R_STRING);

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
				System.err.println("external program " + binary.getCommand() + " found at " + binary.getLocation());
			else
				System.err.println("external program " + binary.getCommand() + " not found");
		}
	}

	public static String getOpenBabelVersion()
	{
		if (!BABEL_BINARY.isFound())
			throw new IllegalStateException();
		if (babelVersion == null)
		{
			File bVersion = null;
			try
			{
				bVersion = File.createTempFile("babel", "version");
				ExternalTool ext = new ExternalTool();
				ext.run("babel", BABEL_BINARY.getLocation() + " -V", bVersion, true);
				BufferedReader b = new BufferedReader(new FileReader(bVersion));
				String s;
				Pattern pattern = Pattern.compile("^.*([0-9]+\\.[0-9]+\\.[0-9]+).*$");
				while ((s = b.readLine()) != null)
				{
					Matcher matcher = pattern.matcher(s);
					if (matcher.matches())
					{
						babelVersion = matcher.group(1);
						break;
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				bVersion.delete();
			}
		}
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
		BinaryLocator.locate(bins);
	}

	public static void showBinaryDialog(Binary select)
	{
		locateBinarys();
		new BinaryLocatorDialog((Window) Settings.TOP_LEVEL_FRAME, "External Programs", Settings.TITLE, bins, select);
		for (Binary binary : bins)
			if (binary.getLocation() != null)
				PropHandler.put("bin-path-" + binary.getCommand(), binary.getLocation());
			else
				PropHandler.remove("bin-path-" + binary.getCommand());
		PropHandler.storeProperties();
	}

	public static JComponent getBinaryComponent(final Binary bin)
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
					l.setIcon(ImageLoader.TOOL);
			}
		});
		if (!bin.isFound())
			l.setIcon(ImageLoader.ERROR);
		else
			l.setIcon(ImageLoader.TOOL);
		l.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				showBinaryDialog(bin);
			}
		});
		return l;
	}
}
