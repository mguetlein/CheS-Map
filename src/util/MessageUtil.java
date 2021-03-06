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
	public static Action createURLAction(final String link)
	{
		return new AbstractAction("more...")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					Desktop.getDesktop().browse(new URI(link));
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		};
	}

	public static Messages slowRuntimeMessages(String msg)
	{
		return Messages.slowMessage(msg, createURLAction(Settings.HOMEPAGE_RUNTIME));
	}

	public static Message slowRuntimeMessage(String msg)
	{
		return Message.slowMessage(msg, createURLAction(Settings.HOMEPAGE_RUNTIME));
	}
}
