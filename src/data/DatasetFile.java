package data;

import java.util.ArrayList;
import java.util.List;

import main.Settings;
import main.TaskProvider;

import org.openscience.cdk.interfaces.IMolecule;

import util.FileUtil;

public class DatasetFile
{
	private String URI;
	private String localPath;

	private String sdfPath;
	private String sdf3DPath;
	private String md5;
	private String shortName;
	private String fullName;
	private String name;

	private String extension;

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

	public String getFullName()
	{
		return fullName;
	}

	private String getNameFromURL(boolean withExtension)
	{
		String s[] = URI.split("/");
		int i = s.length - 1;
		while (s[i].trim().length() == 0 && i > 0)
			i--;
		String ss = s[i];
		if (withExtension)
			return ss;
		int index = ss.lastIndexOf('.');
		if (index > 0)
			ss = ss.substring(0, index);
		return ss;
	}

	public String getName()
	{
		if (name == null)
		{
			if (isLocal())
				name = FileUtil.getFilename(localPath, true);
			else
				name = getNameFromURL(true);
		}
		return name;
	}

	public String getShortName()
	{
		if (shortName == null)
		{
			if (isLocal())
				shortName = FileUtil.getFilename(localPath, false);
			else
				shortName = getNameFromURL(false);
		}
		return shortName;
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

	private DatasetFile(String uRI, String localPath, String fullName)
	{
		this.URI = uRI;
		this.localPath = localPath;
		this.fullName = fullName;
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
			Settings.LOGGER.info("new dataset " + d.getShortName());
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
		return URI + "#" + localPath + "#" + fullName;
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

	public IntegratedProperty getIntegratedClusterProperty()
	{
		return featureService.getIntegratedClusterProperty(this);
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
			if (f.localPath.equals(sdfFile) || sdfFile.equals(f.getSDFPath(true)))
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
		{
			if (!TaskProvider.isRunning())
				return;
			TaskProvider.verbose("Creating 2D structures");
			FeatureService.writeMoleculesToSDFFile(this, Settings.destinationSDFFile(this));
			this.setSDFPath(Settings.destinationSDFFile(this), false);
			this.updateMoleculesStructure(false);
		}
	}

	public int numCompounds()
	{
		return featureService.numCompounds(this);
	}

	public void updateMoleculesStructure(boolean threeD)
	{
		featureService.updateMoleculesStructure(this, threeD);
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
			md5 = FileUtil.getMD5String(localPath);
		return md5;
	}

	public void setFileExtension(String ext)
	{
		this.extension = ext;
	}

	public String getFileExtension()
	{
		if (extension == null)
			extension = FileUtil.getFilenamExtension(localPath);
		return extension;
	}
}
