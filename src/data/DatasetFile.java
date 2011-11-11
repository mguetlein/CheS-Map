package data;

import java.util.ArrayList;
import java.util.List;

import main.Settings;

import org.openscience.cdk.interfaces.IMolecule;

import util.FileUtil;

public class DatasetFile
{
	private String URI;
	private String localPath;
	private String name;
	private String sdfPath;
	private String sdf3DPath;
	private String md5;

	public boolean isLocal()
	{
		return URI == null;
	}

	public String getURI()
	{
		return URI;
	}

	public String getLocalPath()
	{
		return localPath;
	}

	public String getName()
	{
		return name;
	}

	public String getSDFPath(boolean threeD)
	{
		if (threeD)
			return sdf3DPath;
		else
			return sdfPath;
	}

	public void setSDFPath(String sdfPath, boolean threeD)
	{
		if (threeD)
			this.sdf3DPath = sdfPath;
		else
			this.sdfPath = sdfPath;
	}

	private DatasetFile(String uRI, String localPath, String name)
	{
		this.URI = uRI;
		this.localPath = localPath;
		this.name = name;
	}

	public static DatasetFile localFile(String localPath)
	{
		return uniqueDatasetFile(new DatasetFile(null, localPath, FileUtil.getFilename(localPath)));
	}

	public static DatasetFile getURLDataset(String uRI)
	{
		return uniqueDatasetFile(new DatasetFile(uRI, Settings.destinationFileForURL(uRI), uRI));
	}

	private static List<DatasetFile> instances = new ArrayList<DatasetFile>();

	private static DatasetFile uniqueDatasetFile(DatasetFile d)
	{
		int index = instances.indexOf(d);
		if (index == -1)
		{
			instances.add(d);
			return d;
		}
		else
			return instances.get(index);
	}

	public static DatasetFile fromString(String s)
	{
		String ss[] = s.split("#");
		return new DatasetFile(ss[0].equals("null") ? null : ss[0], ss[1], ss[2]);
	}

	/**
	 * textfield
	 * 
	 * @return
	 */
	public String getPath()
	{
		if (isLocal())
			return localPath;
		else
			return URI;
	}

	public String toString()
	{
		return URI + "#" + localPath + "#" + name;
	}

	public boolean equals(Object o)
	{
		if (o instanceof DatasetFile)
			return toString().equals(((DatasetFile) o).toString());
		else
			return false;
	}

	// -------------------- loading stuff ---------------------

	final static FeatureService featureService = new FeatureService();

	public IntegratedProperty[] getIntegratedProperties(boolean includingSmiles)
	{
		return featureService.getIntegratedProperties(this, includingSmiles);
	}

	public IntegratedProperty[] getIntegratedClusterProperties()
	{
		return featureService.getIntegratedClusterProperties(this);
	}

	public boolean isLoaded()
	{
		return featureService.isLoaded(this);
	}

	private void clear()
	{
		featureService.clear(this);
	}

	public static void clearFilesWith3DSDF(String sdfFile)
	{
		for (DatasetFile f : instances)
		{
			if (f.getLocalPath().equals(sdfFile) || sdfFile.equals(f.getSDFPath(true)))
				f.clear();
		}
	}

	public void loadDataset() throws Exception
	{
		loadDataset(true);
	}

	public void loadDataset(boolean loadHydrogen) throws Exception
	{
		featureService.loadDataset(this, loadHydrogen);
		if (getSDFPath(false) == null)
			FeatureService.writeSDFFile(this);
	}

	public int numCompounds()
	{
		return featureService.numCompounds(this);
	}

	public IMolecule[] getMolecules()
	{
		return featureService.getMolecules(this);
	}

	public IMolecule[] getMolecules(boolean loadHydrogen)
	{
		return featureService.getMolecules(this, loadHydrogen);
	}

	// -------------------------------

	public String[] getSmiles()
	{
		return featureService.getSmiles(this);
	}

	public boolean has3D()
	{
		return featureService.has3D(this);
	}

	public String getMD5()
	{
		if (md5 == null)
			md5 = FileUtil.getMD5String(getLocalPath());
		return md5;
	}
}
