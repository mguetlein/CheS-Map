package alg.embed3d.r;

import main.Settings;
import alg.embed3d.AbstractRTo3DEmbedder;

public class PCAFeature3DEmbedder extends AbstractRTo3DEmbedder
{
	public static final PCAFeature3DEmbedder INSTANCE = new PCAFeature3DEmbedder();

	private PCAFeature3DEmbedder()
	{
	}

	public int getMinNumFeatures()
	{
		return 1;
	}

	@Override
	public int getMinNumInstances()
	{
		return 2;
	}

	@Override
	public String getShortName()
	{
		return "pca";
	}

	@Override
	public String getName()
	{
		return getNameStatic();
	}

	public static String getNameStatic()
	{
		return Settings.text("embed.r.pca");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("embed.r.pca.desc", Settings.R_STRING);
	}

	@Override
	protected String getRScriptCode()
	{
		return "args <- commandArgs(TRUE)\n" //
				+ "df = read.table(args[1])\n" //
				// + "res <- princomp(df)\n" //
				// + "print(res$scores[,1:3])\n" //
				// + "write.table(res$scores[,1:3],args[2]) ";
				+ "res <- prcomp(df)\n" //
				+ "rows <-min(ncol(res$x),3)\n" //
				+ "print(head(res$x[,1:rows]))\n" //
				+ "write.table(res$x[,1:rows],args[2])\n"//
				+ "write.table(res$rotation[,1:rows],args[3])\n";
	}

	@Override
	public boolean isLinear()
	{
		return true;
	}

	@Override
	public boolean isLocalMapping()
	{
		return false;
	}

	@Override
	protected String getErrorDescription(String errorOut)
	{
		return null;
	}

}
