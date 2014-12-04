package org.chesmapper.map.data;

import java.util.ArrayList;
import java.util.List;

import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.chesmapper.map.property.IntegratedPropertySet;
import org.chesmapper.map.property.PropertySetProvider;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.StringUtil;
import org.openscience.cdk.interfaces.IAtomContainer;

public class DatasetFile extends FilenameProvider
{
	private String URI;
	private String localPath;

	private String SDF;
	private String SDF3D;
	private String SDFClustered;
	private String SDFAligned;
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

	public String getSDF()
	{
		return SDF;
	}

	public void setSDF(String sdf)
	{
		this.SDF = sdf;
	}

	public String getSDF3D()
	{
		return SDF3D;
	}

	public void setSDF3D(String sdf3d)
	{
		SDF3D = sdf3d;
	}

	public String getSDFClustered()
	{
		return SDFClustered;
	}

	public void setSDFClustered(String sDFClustered)
	{
		SDFClustered = sDFClustered;
	}

	public String getSDFAligned()
	{
		return SDFAligned;
	}

	public void setSDFAligned(String sdfAligned)
	{
		this.SDFAligned = sdfAligned;
	}

	private DatasetFile(String uRI, String localPath, String fullName)
	{
		this.URI = uRI;
		this.localPath = localPath;
		this.fullName = fullName;
		setDatasetFile(this);
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
			//			System.err.println("New dataset " + d.getShortName());
			instances.add(d);
			return d;
		}
		else
		{
			//			System.err.println("Old dataset " + d.getShortName());
			return instances.get(index);
		}
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

	public IntegratedPropertySet[] getIntegratedProperties()
	{
		return featureService.getIntegratedProperties(this);
	}

	public IntegratedPropertySet getIntegratedClusterProperty()
	{
		return featureService.getIntegratedClusterProperty(this);
	}

	public boolean isLoaded()
	{
		return featureService.isLoaded(this);
	}

	public void clear()
	{
		PropertySetProvider.INSTANCE.clearComputedProperties(this);
		featureService.clear(this);
		instances.remove(this);
	}

	public static void clearFiles(String sdfFile)
	{
		for (DatasetFile f : instances)
			if (sdfFile.equals(f.getSDF()) || sdfFile.equals(f.getSDF3D()) || sdfFile.equals(f.getSDFClustered())
					|| sdfFile.equals(f.getSDFAligned()))
				featureService.clear(f);
	}

	public void loadDataset() throws Exception
	{
		featureService.loadDataset(this);
		if (getSDF() == null)
		{
			if (!TaskProvider.isRunning())
				return;
			TaskProvider.debug("Creating 2D structures");
			FeatureService.writeCompoundsToSDFile(this, Settings.destinationFile(this, "sdf"));
			this.setSDF(Settings.destinationFile(this, "sdf"));
			this.updateCompoundStructureFrom2DSDF();
		}
	}

	public int numCompounds()
	{
		return featureService.numCompounds(this);
	}

	public void updateCompoundStructureFrom2DSDF()
	{
		featureService.updateCompoundStructureFrom2DSDF(this);
	}

	public void updateCompoundStructureFrom3DSDF()
	{
		featureService.updateCompoundStructureFrom3DSDF(this);
	}

	public IAtomContainer[] getCompounds()
	{
		return featureService.getCompounds(this);
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
			md5 = StringUtil.getMD5(getShortName() + "." + FileUtil.getMD5String(localPath));
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
