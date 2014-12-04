package org.chesmapper.map.alg.build3d;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import javax.vecmath.Point3d;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.mg.javalib.io.SDFUtil;
import org.mg.javalib.io.SDFUtil.SDChecker;
import org.mg.javalib.util.FileUtil;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

public abstract class AbstractReal3DBuilder extends Abstract3DBuilder
{
	private boolean running = false;

	private String threeDFilename;

	public abstract boolean[] build3D(DatasetFile dataset, String outFile) throws Exception;

	public String get3DSDFile()
	{
		return threeDFilename;
	}

	@Override
	public boolean isCached(DatasetFile dataset)
	{
		File threeD = new File(dataset.get3DBuilderSDFilePath(this));
		if (threeD.exists())
			Settings.LOGGER.info("3d file already exists: " + threeD);
		return threeD.exists();
	}

	public enum AutoCorrect
	{
		disabled, external, sdf2D
	}

	AutoCorrect autoCorrect = AutoCorrect.disabled;

	public void setAutoCorrect(AutoCorrect a)
	{
		autoCorrect = a;
	}

	@Override
	public void build3D(final DatasetFile dataset) throws Exception
	{
		String sdfFile = dataset.getSDF();
		File orig = new File(sdfFile);
		if (!orig.exists())
			throw new IllegalStateException("sdf file not found");

		try
		{
			final File tmpFile = File.createTempFile("3dbuild", "tmp");
			String finalFile = dataset.get3DBuilderSDFilePath(this);

			File threeD = new File(finalFile);
			if (!threeD.exists() || !Settings.CACHING_ENABLED)
			{
				Settings.LOGGER.info("computing 3d: " + finalFile);
				running = true;
				final int max = SDFUtil.countCompounds(sdfFile);
				Thread th = new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						while (running)
						{
							try
							{
								Thread.sleep(3000);
							}
							catch (InterruptedException e)
							{
								Settings.LOGGER.error(e);
							}
							if (tmpFile.exists())
							{
								int i = SDFUtil.countCompounds(tmpFile.getAbsolutePath());
								TaskProvider.update("Compute 3D structure for compound " + (i + 1) + "/" + max
										+ " (the results are cached)");
							}
						}
					}
				});
				th.start();
				boolean[] build3dsuccessfull = build3D(dataset, tmpFile.getAbsolutePath());
				running = false;

				if (autoCorrect == AutoCorrect.sdf2D)
					check3DSDFile(tmpFile.getAbsolutePath(), dataset.getSDF(), tmpFile.getAbsolutePath(),
							build3dsuccessfull);
				else if (autoCorrect == AutoCorrect.external)
				{
					if (dataset.getLocalPath() != null && dataset.getLocalPath().toLowerCase().endsWith(".smi"))
						check3DSDFileExternal(tmpFile.getAbsolutePath(), dataset.getLocalPath(),
								tmpFile.getAbsolutePath(), build3dsuccessfull);
					else
						check3DSDFileExternal(tmpFile.getAbsolutePath(), dataset.getSmiles(),
								tmpFile.getAbsolutePath(), build3dsuccessfull);
				}

				if (!TaskProvider.isRunning())
					return;

				if (!FileUtil.robustRenameTo(tmpFile, new File(finalFile)))
					throw new Error("renaming or delete file error");
			}
			else
				Settings.LOGGER.info("3d already computed: " + finalFile);
			threeDFilename = finalFile;
		}
		finally
		{
			running = false;
		}
	}

	static SDChecker sdCheck = new SDChecker()
	{
		@Override
		public boolean invalid(String compoundString, int sdFileIndex)
		{
			MDLV2000Reader reader = null;
			try
			{
				int numAtoms = -1;
				for (String line : compoundString.split("\n"))
					if (line.contains("V2000"))
					{
						numAtoms = Integer.parseInt(line.substring(0, 3).trim());
						break;
					}
				if (numAtoms == -1)
					throw new Exception("could not parse num atoms");
				reader = new MDLV2000Reader(new InputStreamReader(new ByteArrayInputStream(compoundString.getBytes())));
				IChemFile content = (IChemFile) reader.read((IChemObject) new ChemFile());
				List<IAtomContainer> list = ChemFileManipulator.getAllAtomContainers(content);
				if (list.size() != 1)
					throw new Exception("Cannot parse compound");
				if (list.get(0).getAtomCount() != numAtoms)
					throw new Exception("Num atoms " + list.get(0).getAtomCount() + " != " + numAtoms);
				for (int i = 0; i < list.get(0).getBondCount(); i++)
				{
					if (list.get(0).getBond(i).getAtomCount() != 2)
						throw new Exception("Num atoms for bond is " + list.get(0).getBond(i).getAtomCount());
					IAtom a = list.get(0).getBond(i).getAtom(0);
					IAtom b = list.get(0).getBond(i).getAtom(1);
					Point3d pa = a.getPoint3d();
					if (pa == null)
						pa = new Point3d(a.getPoint2d().x, a.getPoint2d().y, 0.0);
					Point3d pb = b.getPoint3d();
					if (pb == null)
						pb = new Point3d(b.getPoint2d().x, b.getPoint2d().y, 0.0);
					double d = pa.distance(pb);
					if (d < 0.9 || d > 2.5)
						throw new Exception("Distance between atoms is " + d);
				}
				return false;
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return true;
			}
			finally
			{
				try
				{
					if (reader != null)
						reader.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	};

	public static class CombinedSDFChecker implements SDChecker
	{
		boolean valid[];

		public CombinedSDFChecker(boolean valid[])
		{
			this.valid = valid;
		}

		@Override
		public boolean invalid(String compoundString, int sdFileIndex)
		{
			if (valid != null && !valid[sdFileIndex])
				return true;
			else
				return sdCheck.invalid(compoundString, sdFileIndex);
		}
	}

	public static void check3DSDFile(String sdf3Dcorrupt, String sdf2Dcorrect, String sdfResult, boolean[] valid)
	{
		SDFUtil.checkSDFile(sdf3Dcorrupt, sdf2Dcorrect, sdfResult, new CombinedSDFChecker(valid));
	}

	public static void check3DSDFileExternal(String sdf3Dcorrupt, String smiFile, String sdfResult, boolean[] valid)
	{
		SDFUtil.checkSDFileExternal(sdf3Dcorrupt, smiFile, sdfResult, new CombinedSDFChecker(valid));
	}

	public static void check3DSDFileExternal(String sdf3Dcorrupt, String smiles[], String sdfResult, boolean[] valid)
	{
		SDFUtil.checkSDFileExternal(sdf3Dcorrupt, smiles, sdfResult, new CombinedSDFChecker(valid));
	}

	@Override
	public boolean isReal3DBuilder()
	{
		return true;
	}

}
