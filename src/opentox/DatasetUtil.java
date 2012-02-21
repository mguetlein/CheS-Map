package opentox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.swing.JOptionPane;

import main.Settings;

public class DatasetUtil
{
	public static boolean isAmbitURI(String uri)
	{
		return (uri.contains("apps.ideaconsult.net") || uri.contains("ambit.uni-plovdiv.bg"));
	}

	public static String AMBIT_DATASET_SERVICE_URI = "http://apps.ideaconsult.net:8080/ambit2/dataset";

	public static String uploadDatasetToAmbit(String filename)
	{
		return RESTUtil.postFile(AMBIT_DATASET_SERVICE_URI, filename);
	}

	public static void main(String args[])
	{
		System.out.println(uploadDatasetToAmbit("/home/martin/data/ches-mapper/chang.sdf"));
	}

	public static boolean downloadDataset(String datasetUrl)
	{
		try
		{
			URL url = new URL(datasetUrl);
			File f = new File(Settings.destinationFileForURL(datasetUrl));
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("GET");
			if (isAmbitURI(datasetUrl))
				connection.setRequestProperty("accept", "chemical/x-mdl-sdf");
			else
				connection.setRequestProperty("accept", "text/csv");
			BufferedWriter buffy = new BufferedWriter(new FileWriter(f));
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String s = "";
			while ((s = reader.readLine()) != null)
				buffy.write(s + "\n");
			buffy.flush();
			buffy.close();
			reader.close();
			connection.disconnect();
			if (connection.getResponseCode() >= 400)
				throw new Exception("Response code: " + connection.getResponseCode());
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, "Error: could not load dataset from: '"
					+ datasetUrl + "'\nError type: '" + e.getClass().getSimpleName() + "'\nMessage: '" + e.getMessage()
					+ "'", "Http Connection Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

	}
}
