package org.chesmapper.map.alg.cluster.r;

import org.chesmapper.map.alg.DistanceMeasure;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.rscript.RScriptUtil;

class ModelBasedRClusterer extends AbstractRClusterer
{
	public static final ModelBasedRClusterer INSTANCE = new ModelBasedRClusterer();

	private ModelBasedRClusterer()
	{
	}

	@Override
	public String getName()
	{
		return "Model Based (R)";
	}

	@Override
	public String getDescription()
	{
		return "Uses " + Settings.R_STRING + ".\n\n" + "bla.\n\n" + "http://.html";
	}

	@Override
	protected String getShortName()
	{
		return "mclust";
	}

	@Override
	protected String getRScriptCode()
	{
		return "args <- commandArgs(TRUE)\n" //
				+ "\n" + RScriptUtil.installAndLoadPackage("mclust")
				+ "\n"
				+ "df = read.table(args[1])\n"
				+ "set.seed(1)\n" //
				+ "res <- Mclust(df)\n" + "print(res$classification)\n" + "\n" + "print(res$loglik)\n"
				+ "\n"
				+ "write.table(res$classification,args[2])\n";
	}

	@Override
	public DistanceMeasure getDistanceMeasure()
	{
		return DistanceMeasure.UNKNOWN_DISTANCE;
	}
}