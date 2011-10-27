package alg;

public class Message
{
	String string;
	MessageType type;

	public Message(String string, MessageType type)
	{
		this.string = string;
		this.type = type;
	}

	public static Message infoMessage(String string)
	{
		return new Message(string, MessageType.Info);
	}

	public static Message warningMessage(String string)
	{
		return new Message(string, MessageType.Warning);
	}

	public static Message errorMessage(String string)
	{
		return new Message(string, MessageType.Error);
	}

	public String getString()
	{
		return string;
	}

	public MessageType getType()
	{
		return type;
	}
}
