package alg.build3d;

import io.SDFUtil;
import io.SDFUtil.SDChecker;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import javax.vecmath.Point3d;

import main.Settings;
import main.TaskProvider;

import org.openscience.cdk.ChemFile;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

import data.DatasetFile;

public abstract class AbstractReal3DBuilder extends Abstract3DBuilder
{
	private boolean running = false;

	private String threeDFilename;

	public abstract void build3D(DatasetFile dataset, String outFile);

	public String get3DSDFFile()
	{
		return threeDFilename;
	}

	public abstract String getInitials();

	private String destinationFile(DatasetFile dataset)
	{
		return Settings.destinationFile(dataset, dataset.getShortName() + "." + dataset.getMD5() + "." + getInitials()
				+ "3d.sdf");
	}

	@Override
	public boolean isCached(DatasetFile dataset)
	{
		File threeD = new File(destinationFile(dataset));
		if (threeD.exists())
			Settings.LOGGER.info("3d file already exists: " + threeD);
		return threeD.exists();
	}

	@Override
	public void build3D(final DatasetFile dataset)
	{
		if (Settings.CACHING_ENABLED && dataset.getSDFPath(false).contains("." + getInitials() + "3d"))
		{
			Settings.LOGGER.info("file already in " + getInitials() + "3d : " + dataset.getSDFPath(false)
					+ ", no 3d structure generation");
			threeDFilename = dataset.getSDFPath(false);
			return;
		}

		String sdfFile = dataset.getSDFPath(false);
		File orig = new File(sdfFile);
		if (!orig.exists())
			throw new IllegalStateException("sdf file not found");

		try
		{
			final File tmpFile = File.createTempFile("3dbuild", "tmp");
			String finalFile = destinationFile(dataset);

			//			Settings.LOGGER.println(threeDFilename);
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
								TaskProvider.update("Building 3D structure for compound " + (i + 1) + "/" + max);
								TaskProvider
										.verbose("This may take some time. The result is cached, you have to do it only once.");
							}
						}
					}
				});
				th.start();
				build3D(dataset, tmpFile.getAbsolutePath());
				running = false;

				SDChecker sdCheck = new SDChecker()
				{
					@Override
					public boolean invalid(String moleculeString)
					{
						try
						{
							int numAtoms = -1;
							for (String line : moleculeString.split("\n"))
								if (line.contains("V2000"))
								{
									numAtoms = Integer.parseInt(line.substring(0, 3).trim());
									break;
								}
							if (numAtoms == -1)
								throw new Exception("could not parse num atoms");
							MDLV2000Reader reader = new MDLV2000Reader(new InputStreamReader(new ByteArrayInputStream(
									moleculeString.getBytes())));
							IChemFile content = (IChemFile) reader.read((IChemObject) new ChemFile());
							List<IAtomContainer> list = ChemFileManipulator.getAllAtomContainers(content);
							if (list.size() != 1)
								throw new Exception("Cannot parse molecule");
							if (list.get(0).getAtomCount() != numAtoms)
								throw new Exception("Num atoms " + list.get(0).getAtomCount() + " != " + numAtoms);
							for (int i = 0; i < list.get(0).getBondCount(); i++)
							{
								if (list.get(0).getBond(i).getAtomCount() != 2)
									throw new Exception("Num atoms for bond is "
											+ list.get(0).getBond(i).getAtomCount());
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
					}
				};
				SDFUtil.checkSDFile(tmpFile.getAbsolutePath(), dataset.getSDFPath(false), tmpFile.getAbsolutePath(),
						sdCheck);

				if (!TaskProvider.isRunning())
					return;
				boolean res = tmpFile.renameTo(new File(finalFile));
				res |= tmpFile.delete();
				if (!res)
					throw new Error("renaming or delete file error");
			}
			else
				Settings.LOGGER.info("3d already computed: " + finalFile);
			threeDFilename = finalFile;
		}
		catch (IOException e1)
		{
			Settings.LOGGER.error(e1);
		}
	}

	@Override
	public boolean isReal3DBuilder()
	{
		return true;
	}

}
