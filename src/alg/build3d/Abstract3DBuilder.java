package alg.build3d;

import gui.Progressable;
import io.SDFUtil;

import java.io.File;
import java.io.IOException;

import main.Settings;
import util.FileUtil;
import data.DatasetFile;

public abstract class Abstract3DBuilder implements ThreeDBuilder
{
	private boolean running = false;

	private String threeDFilename;

	public abstract void build3D(DatasetFile dataset, String outFile);

	public String get3DSDFFile()
	{
		return threeDFilename;
	}

	@Override
	public String getPreconditionErrors()
	{
		return null;
	}

	public abstract String getInitials();

	@Override
	public void build3D(final DatasetFile dataset, final Progressable progress)
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
		int index = sdfFile.lastIndexOf('.');
		if (index == -1)
			throw new IllegalStateException("filename has no '.'");

		try
		{
			final File tmpFile = File.createTempFile("3dbuild", "tmp");
			String finalFile = Settings.destinationFile(sdfFile, FileUtil.getFilename(sdfFile, false) + "."
					+ getInitials() + "3d" + sdfFile.substring(index));

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
								progress.update(i / (double) max * 100, i + "/" + max + " 3D structure generated");
							}
						}
					}
				});
				th.start();
				build3D(dataset, tmpFile.getAbsolutePath());
				running = false;

				if (Settings.isAborted(Thread.currentThread()))
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
