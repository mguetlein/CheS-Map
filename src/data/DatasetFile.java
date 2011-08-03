package data;

import gui.Progressable;

import java.util.ArrayList;
import java.util.List;

import main.Settings;

import org.openscience.cdk.interfaces.IMolecule;

import util.ArrayUtil;
import util.FileUtil;
import dataInterface.MoleculeProperty;

public class DatasetFile
{
	private String URI;
	private String localPath;
	private String name;
	private String sdfPath;
	private String sdf3DPath;

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

	final static CDKService cdkService = new CDKService();

	public IntegratedProperty[] getIntegratedProperties(boolean includingSmiles)
	{
		return cdkService.getIntegratedProperties(this, includingSmiles);
	}

	public IntegratedProperty[] getIntegratedNumericProperties()
	{
		return cdkService.getIntegratedNumericProperties(this);
	}

	public IntegratedProperty[] getIntegratedClusterProperties()
	{
		return cdkService.getIntegratedClusterProperties(this);
	}

	public boolean isLoaded()
	{
		return cdkService.isLoaded(this);
	}

	private void clear()
	{
		cdkService.clear(this);
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
		loadDataset(true, null);
	}

	public void loadDataset(final boolean loadHydrogen) throws Exception
	{
		loadDataset(loadHydrogen, null);
	}

	public void loadDataset(boolean loadHydrogen, final Progressable progress) throws Exception
	{
		cdkService.loadDataset(this, loadHydrogen, progress);
	}

	public int numCompounds()
	{
		return cdkService.numCompounds(this);
	}

	public IMolecule[] getMolecules()
	{
		return cdkService.getMolecules(this);
	}

	public IMolecule[] getMolecules(boolean loadHydrogen)
	{
		return cdkService.getMolecules(this, loadHydrogen);
	}

	public String[] getStringValues(MoleculeProperty p)
	{
		return ArrayUtil.cast(String.class, getValues(p, false));
	}

	public Object[] getObjectValues(MoleculeProperty p)
	{
		return getValues(p, false);
	}

	public Double[] getDoubleValues(MoleculeProperty p, boolean normalize)
	{
		return ArrayUtil.cast(Double.class, getValues(p, normalize));
	}

	private Object[] getValues(MoleculeProperty p, boolean normalize)
	{
		return cdkService.getValues(this, p, normalize);
	}

	public String[] getSmiles()
	{
		return cdkService.getSmiles(this);
	}

	public boolean has3D()
	{
		return cdkService.has3D(this);
	}

}
