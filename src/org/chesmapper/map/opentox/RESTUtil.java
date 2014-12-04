package org.chesmapper.map.opentox;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import javax.swing.JOptionPane;

import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.mg.javalib.util.StringUtil;

public class RESTUtil
{
	private static String token;

	private static String getToken()
	{
		if (token == null)
			token = getToken("guest", "guest");
		return token;
	}

	private static String getToken(String username, String password)
	{
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("username", username);
		params.put("password", password);
		String result = post("http://opensso.in-silico.ch/auth/authenticate", params);
		return result.replace("token.id=", "").trim();
	}

	private static long lastMsg = -1;

	private static String waitFor(HttpURLConnection connection, long start) throws IOException, IllegalStateException
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
			if (start == -1)
				start = new Date().getTime();
			if (lastMsg == -1 || (new Date().getTime() - lastMsg) > 10000)
			{
				lastMsg = new Date().getTime();
				Settings.LOGGER.info(StringUtil.formatTime(new Date().getTime() - start) + " waiting for task "
						+ b.toString());
			}
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				Settings.LOGGER.error(e);
			}
			if (!TaskProvider.isRunning())
				return null;

			HashMap<String, String> params = new HashMap<String, String>();
			params.put("accept", "text/uri-list");
			return get(b.toString(), params, start);
		}
		else
		{
			Settings.LOGGER.info("result: " + b.toString());
			return b.toString();
		}
	}

	public static void delete(String urlString)
	{
		HttpURLConnection connection = null;
		try
		{
			Settings.LOGGER.info("deleting: " + urlString);

			URL url = new URL(urlString);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("DELETE");
			connection.setRequestProperty("Subjectid", URLEncoder.encode(getToken(), "UTF-8"));
			connection.disconnect();
			if (connection.getResponseCode() >= 400)
				throw new IllegalStateException("Response code: " + connection.getResponseCode());
		}
		catch (Exception e)
		{
			Settings.LOGGER.error(e);

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
			JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, "Error: could not delete: '" + urlString
					+ "'\nError type: '" + e.getClass().getSimpleName() + "'\nMessage: '" + e.getMessage()
					+ "'\n\nServer Error:\n" + serverError, "Http Connection Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public static String get(String urlString, HashMap<String, String> headers)
	{
		return get(urlString, headers, -1);
	}

	public static String get(String urlString, HashMap<String, String> headers, long start)
	{
		HttpURLConnection connection = null;
		try
		{
			URL url = new URL(urlString);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Subjectid", URLEncoder.encode(getToken(), "UTF-8"));
			if (headers != null)
				for (String key : headers.keySet())
					connection.setRequestProperty(key, headers.get(key));
			return waitFor(connection, start);
		}
		catch (Exception e)
		{
			Settings.LOGGER.error(e);

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
			JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, "Error: could wait for task: '" + urlString
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
			if (!urlString.contains("authenticate"))
				connection.setRequestProperty("Subjectid", URLEncoder.encode(getToken(), "UTF-8"));
			connection.setRequestMethod("POST");
			DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
			Settings.LOGGER.info("POST " + p + " " + urlString);

			String curl_call = "curl ";
			for (String key : params.keySet())
				curl_call += "-d " + key + "=" + params.get(key) + " ";
			curl_call += urlString;
			if (!urlString.contains("authenticate"))
				curl_call += " -H \"subjectid=" + getToken() + "\"";
			Settings.LOGGER.info("(for debbuging: '" + curl_call + "')");

			dos.writeBytes(p);
			dos.flush();
			dos.close();
			return waitFor(connection, -1);
		}
		catch (Exception e)
		{
			Settings.LOGGER.error(e);
			JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, "Error: could not post to: '" + urlString
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
			Settings.LOGGER.info("randomMod: " + randomMod);

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
			connection.setRequestProperty("Subjectid", URLEncoder.encode(getToken(), "UTF-8"));
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

			return waitFor(connection, -1);
		}
		catch (Exception e)
		{
			Settings.LOGGER.error(e);
			JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, "Error: could not post to: '" + urlString
					+ "'\nError type: '" + e.getClass().getSimpleName() + "'\nMessage: '" + e.getMessage() + "'",
					"Http Connection Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}

	}

	public static void main(String args[])
	{
		//		HashMap<String, String> params = new HashMap<String, String>();
		//		params.put("accept", "text/uri-list");
		//		Settings.LOGGER.println(get("http://local-ot/task/10", params));

		Settings.LOGGER.info(getToken("guest", "guest"));
	}

}
