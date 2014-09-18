package util;

import gui.Message;
import gui.Messages;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.net.URI;

import javax.swing.AbstractAction;
import javax.swing.Action;

import main.Settings;

public class MessageUtil
{
	static Action RUNTIME_ACTION = new AbstractAction("more...")
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			try
			{
				Desktop.getDesktop().browse(new URI(Settings.HOMEPAGE_RUNTIME));
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
	};

	public static Messages slowRuntimeMessages(String msg)
	{
		return Messages.slowMessage(msg, RUNTIME_ACTION);
	}

	public static Message slowRuntimeMessage(String msg)
	{
		return Message.slowMessage(msg, RUNTIME_ACTION);
	}
}
