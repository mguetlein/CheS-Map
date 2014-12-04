package org.chesmapper.map.alg.embed3d.r;

import org.chesmapper.map.alg.DistanceMeasure;
import org.chesmapper.map.alg.embed3d.AbstractRTo3DEmbedder;
import org.chesmapper.map.main.Settings;
import org.mg.javalib.util.StringLineAdder;

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
		StringLineAdder s = new StringLineAdder();
		s.add("args <- commandArgs(TRUE)");
		s.add("df = read.table(args[1])");
		s.add("res <- prcomp(df)");
		s.add("rows <-min(ncol(res$x),3)");
		s.add("print(head(res$x[,1:rows]))");
		s.add("write.table(res$x[,1:rows],args[2])");
		s.add("write.table(as.matrix(dist(df, method = \"euclidean\")),args[3])");
		s.add("write.table(res$rotation[,1:rows],args[4])");
		return s.toString();
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

	@Override
	public DistanceMeasure getDistanceMeasure()
	{
		return DistanceMeasure.EUCLIDEAN_DISTANCE;
	}

}
