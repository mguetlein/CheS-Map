package alg.build3d;

import io.SDFUtil;

import java.io.File;
import java.io.IOException;

import main.Settings;
import main.TaskProvider;
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
		return threeD.exists();
	}

	@Override
	public void build3D(final DatasetFile dataset)
	{
		if (Settings.CACHING_ENABLED && dataset.getSDFPath(false).contains("." + getInitials() + "3d"))
		{
			System.out.println("file already in " + getInitials() + "3d : " + dataset.getSDFPath(false)
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

			//			System.out.println(threeDFilename);
			File threeD = new File(finalFile);
			if (!threeD.exists() || !Settings.CACHING_ENABLED)
			{
				System.out.println("computing 3d: " + finalFile);
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
								e.printStackTrace();
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

				if (!TaskProvider.isRunning())
					return;
				boolean res = tmpFile.renameTo(new File(finalFile));
				res |= tmpFile.delete();
				if (!res)
					throw new Error("renaming or delete file error");
			}
			else
				System.out.println("3d already computed: " + finalFile);
			threeDFilename = finalFile;
		}
		catch (IOException e1)
		{
			e1.printStackTrace();
		}
	}

	@Override
	public boolean isReal3DBuilder()
	{
		return true;
	}

}
