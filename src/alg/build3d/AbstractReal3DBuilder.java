package alg.build3d;

import io.SDFUtil;

import java.io.File;
import java.io.IOException;

import main.Settings;
import main.TaskProvider;
import util.FileUtil;
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
		String sdfFile = dataset.getSDFPath(false);
		int index = sdfFile.lastIndexOf('.');
		if (index == -1)
			throw new IllegalStateException("filename has no '.'");
		return Settings.destinationFile(sdfFile, FileUtil.getFilename(sdfFile, false) + "." + getInitials() + "3d"
				+ sdfFile.substring(index));
	}

	@Override
	public boolean threeDFileAlreadyExists(DatasetFile dataset)
	{
		File threeD = new File(destinationFile(dataset));
		return threeD.exists();
	}

	@Override
	public void build3D(final DatasetFile dataset)
	{
		if (dataset.getSDFPath(false).contains("." + getInitials() + "3d"))
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
			if (!threeD.exists())
			{
				System.out.println("computing 3d: " + finalFile);
				running = true;
				final int max = SDFUtil.countCompounds(sdfFile);
				Thread th = new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						if (!TaskProvider.exists())
							TaskProvider.registerThread("Ches-Mapper-Task");
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
								TaskProvider.task().update("Building 3D structure for compound " + (i + 1) + "/" + max);
								TaskProvider.task().verbose(
										"This may take some time. The result is cached, you have to do it only once.");
							}
						}
					}
				});
				th.start();
				build3D(dataset, tmpFile.getAbsolutePath());
				running = false;

				if (TaskProvider.task().isCancelled())
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
