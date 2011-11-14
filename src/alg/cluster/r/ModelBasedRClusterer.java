package alg.cluster.r;

import main.Settings;
import rscript.RScriptUtil;

class ModelBasedRClusterer extends AbstractRClusterer
{

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
				+ "\n" + RScriptUtil.installAndLoadPackage("mclust") + "\n"
				+ "df = read.table(args[1])\n"
				+ "res <- Mclust(df)\n" + "print(res$classification)\n" + "\n" + "print(res$loglik)\n"
				+ "\n"
				+ "write.table(res$classification,args[2])\n";
	}
}