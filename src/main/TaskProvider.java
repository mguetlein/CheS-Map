package main;

import gui.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
	 * registers a 'key' and 'current thread' for task
	 * 'key' may already exist, 'thread' may NOT yet exist
	 * task is created (if not yet exists for key)  
	 */
	public static void registerThread(String key)
	{
		if (keys.containsKey(Thread.currentThread()))
			throw new Error("thread already registered");
		keys.put(Thread.currentThread(), key);
		if (!tasks.containsKey(key))
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
		List<Thread> toDel = new ArrayList<Thread>();
		for (Thread thread : keys.keySet())
			if (keys.get(thread).equals(key))
				toDel.add(thread);
		for (Thread thread : toDel)
			keys.remove(thread);
		tasks.remove(key);
	}

}
