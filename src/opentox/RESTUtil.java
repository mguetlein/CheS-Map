package opentox;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Random;

import javax.swing.JOptionPane;

import main.Settings;
import util.StringUtil;

public class RESTUtil
{
	private static String waitFor(HttpURLConnection connection) throws IOException, IllegalStateException
	{
		StringBuffer b = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String s = "";
		while ((s = reader.readLine()) != null)
			b.append(s);
		reader.close();
		connection.disconnect();
		if (connection.getResponseCode() >= 400)
			throw new IllegalStateException("Response code: " + connection.getResponseCode());

		if (b.toString().matches("http://.*/task/.*"))
		{
			System.err.println("waiting for task " + b.toString());
			try
			{
				Thread.sleep(300);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			if (Settings.isAborted(Thread.currentThread()))
				return null;

			HashMap<String, String> params = new HashMap<String, String>();
			params.put("accept", "text/uri-list");
			return get(b.toString(), params);
		}
		else
		{
			System.out.println("result: " + b.toString());
			return b.toString();
		}
	}

	public static void delete(String urlString)
	{
		HttpURLConnection connection = null;
		try
		{
			System.err.println("deleting: " + urlString);

			URL url = new URL(urlString);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("DELETE");
			connection.disconnect();
			if (connection.getResponseCode() >= 400)
				throw new IllegalStateException("Response code: " + connection.getResponseCode());
		}
		catch (Exception e)
		{
			e.printStackTrace();

			StringBuffer serverError = new StringBuffer();
			if (connection != null && connection.getErrorStream() != null)
			{
				BufferedReader buffy2 = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
				String s = "";
				try
				{
					while ((s = buffy2.readLine()) != null)
						serverError.append(s + "\n");
				}
				catch (IOException e1)
				{
				}
			}
			JOptionPane.showMessageDialog(Settings.TOP_LEVEL_COMPONENT, "Error: could not delete: '" + urlString
					+ "'\nError type: '" + e.getClass().getSimpleName() + "'\nMessage: '" + e.getMessage()
					+ "'\n\nServer Error:\n" + serverError, "Http Connection Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public static String get(String urlString, HashMap<String, String> headers)
	{
		HttpURLConnection connection = null;
		try
		{
			URL url = new URL(urlString);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("GET");
			if (headers != null)
				for (String key : headers.keySet())
					connection.setRequestProperty(key, headers.get(key));
			return waitFor(connection);
		}
		catch (Exception e)
		{
			e.printStackTrace();

			StringBuffer serverError = new StringBuffer();
			if (connection != null && connection.getErrorStream() != null)
			{
				BufferedReader buffy2 = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
				String s = "";
				try
				{
					while ((s = buffy2.readLine()) != null)
						serverError.append(s + "\n");
				}
				catch (IOException e1)
				{
				}
			}
			JOptionPane.showMessageDialog(Settings.TOP_LEVEL_COMPONENT, "Error: could wait for task: '" + urlString
					+ "'\nError type: '" + e.getClass().getSimpleName() + "'\nMessage: '" + e.getMessage()
					+ "'\n\nServer Error:\n" + serverError, "Http Connection Error", JOptionPane.ERROR_MESSAGE);
		}
		return null;
	}

	public static String post(String urlString, HashMap<String, String> params)
	{
		try
		{
			URL url = new URL(urlString);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			String p = "";
			for (String key : params.keySet())
				p += key + "=" + URLEncoder.encode(params.get(key), "UTF-8") + "&";
			p = p.substring(0, p.length() - 1);
			connection.setRequestProperty("Content-Length", "" + Integer.toString(p.getBytes().length));
			connection.setRequestProperty("Content-Language", "en-US");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setInstanceFollowRedirects(false);
			connection.setRequestProperty("Connection", "Keep-Alive");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("accept", "text/uri-list");
			connection.setRequestMethod("POST");
			DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
			System.err.println("POST " + p + " " + urlString);
			dos.writeBytes(p);
			dos.flush();
			dos.close();
			return waitFor(connection);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(Settings.TOP_LEVEL_COMPONENT, "Error: could not post to: '" + urlString
					+ "'\nError type: '" + e.getClass().getSimpleName() + "'\nMessage: '" + e.getMessage() + "'",
					"Http Connection Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}

	}

	public static String postFile(String urlString, String filename)
	{
		try
		{
			String randomMod = StringUtil.randomString(10, 10, new Random(), false);
			System.err.println("randomMod: " + randomMod);

			String boundary = "*****";
			String lineEnd = "\r\n";
			String twoHyphens = "--";
			int bytesRead, bytesAvailable, bufferSize;
			byte[] buffer;
			int maxBufferSize = 1 * 1024 * 1024;

			URL url = new URL(urlString);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setInstanceFollowRedirects(false);
			connection.setRequestProperty("Connection", "Keep-Alive");
			connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
			connection.setRequestProperty("accept", "text/uri-list");
			connection.setRequestMethod("POST");
			DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
			dos.writeBytes(twoHyphens + boundary + lineEnd);
			dos.writeBytes("Content-Disposition: form-data; name=\"file\";" + " filename=\"" + randomMod
					+ new File(filename).getName() + "\"" + lineEnd);

			dos.writeBytes(lineEnd);
			FileInputStream fileInputStream = new FileInputStream(new File(filename));
			bytesAvailable = fileInputStream.available();
			bufferSize = Math.min(bytesAvailable, maxBufferSize);
			buffer = new byte[bufferSize];
			bytesRead = fileInputStream.read(buffer, 0, bufferSize);
			while (bytesRead > 0)
			{
				dos.write(buffer, 0, bufferSize);
				bytesAvailable = fileInputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);
			}
			dos.writeBytes(lineEnd);
			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
			fileInputStream.close();
			dos.flush();
			dos.close();

			return waitFor(connection);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(Settings.TOP_LEVEL_COMPONENT, "Error: could not post to: '" + urlString
					+ "'\nError type: '" + e.getClass().getSimpleName() + "'\nMessage: '" + e.getMessage() + "'",
					"Http Connection Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}

	}

	public static void main(String args[])
	{
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("accept", "text/uri-list");
		System.out.println(get("http://local-ot/task/10", params));
	}

}
