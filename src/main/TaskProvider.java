package main;

import gui.Task;

import java.util.HashMap;

public class TaskProvider
{
	private static HashMap<Thread, String> keys = new HashMap<Thread, String>();
	private static HashMap<String, Task> tasks = new HashMap<String, Task>();

	public static Task task()
	{
		return task(keys.get(Thread.currentThread()));
	}

	public static void clear()
	{
		clear(keys.remove(Thread.currentThread()));
	}

	public static boolean exists()
	{
		return keys.containsKey(Thread.currentThread());
	}

	/**
	 * registers a new Thread for exiting task with key 'key'
	 */
	public static void registerThread(String key)
	{
		if (!keys.containsValue(key))
			throw new Error("key does not exist");
		keys.put(Thread.currentThread(), key);
	}

	/**
	 * creates a new task with key 'key'
	 * task can be accessed with task('key') or from within the same thread just with task()
	 */
	public static void create(String key)
	{
		if (keys.containsKey(Thread.currentThread()))
			throw new Error("key already exists");
		keys.put(Thread.currentThread(), key);
		if (tasks.containsKey(key))
			throw new Error("task already exists");
		tasks.put(key, new Task(100.0));
	}

	private static Task task(String key)
	{
		if (!tasks.containsKey(key))
			throw new Error("task missing");
		return tasks.get(key);
	}

	private static void clear(String key)
	{
		if (!tasks.containsKey(key))
			throw new Error("task missing");
		tasks.remove(key);
	}

}
