package util;

import gui.Message;
import gui.Messages;
import main.Settings;

public class MessageUtil
{
	public static Messages slowMessages(String msg)
	{
		return Messages.slowMessage(msg, Settings.HOMEPAGE_RUNTIME, "more...");
	}

	public static Message slowMessage(String msg)
	{
		return Message.slowMessage(msg, Settings.HOMEPAGE_RUNTIME, "more...");
	}
}
