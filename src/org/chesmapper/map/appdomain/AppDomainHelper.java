package org.chesmapper.map.appdomain;

import gui.property.PropertyPanel;

import javax.swing.JOptionPane;

import org.chesmapper.map.main.Settings;

import util.SwingUtil;

public class AppDomainHelper
{

	public static AppDomainComputer select()
	{
		AppDomainComputer app = (AppDomainComputer) JOptionPane.showInputDialog(Settings.TOP_LEVEL_FRAME,
				"select app-domain plz", "app-domain", JOptionPane.OK_OPTION, null,
				AppDomainComputer.APP_DOMAIN_COMPUTERS, KNNAppDomainComputer.INSTANCE);
		if (app != null && app.getProperties() != null && app.getProperties().length > 0)
		{
			SwingUtil.showInDialog(new PropertyPanel(app.getProperties()), "configure props for " + app);
		}
		return app;
	}
}
