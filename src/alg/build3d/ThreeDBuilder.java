package alg.build3d;

import gui.Progressable;
import alg.Algorithm;

public interface ThreeDBuilder extends Algorithm
{
	public void build3D(String sdfFile, Progressable progress);

	public String get3DFile();

	public String getDescription();

	public boolean isReal3DBuilder();

	public String getPreconditionErrors();
}
