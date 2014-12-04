package org.chesmapper.map.data;

import java.util.List;

import org.chesmapper.map.alg.Algorithm;
import org.chesmapper.map.alg.build3d.ThreeDBuilder;
import org.chesmapper.map.appdomain.AppDomainComputer;
import org.chesmapper.map.data.fragments.MatchEngine;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertySet;
import org.chesmapper.map.dataInterface.CompoundPropertyUtil;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.property.ListedFragmentProperty;
import org.mg.javalib.gui.property.PropertyUtil;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.StringUtil;

public class FilenameProvider
{
	private ClusteringData clusteringData;
	private DatasetFile dataset;

	public void setDatasetFile(DatasetFile dataset)
	{
		this.dataset = dataset;
	}

	public void setClusteringData(ClusteringData clusteringData)
	{
		this.clusteringData = clusteringData;
	}

	private String getEncodedSettingsIncludingFeatures(Algorithm... algs)
	{
		if (algs == null)
			algs = new Algorithm[0];
		if (ArrayUtil.indexOf(algs, clusteringData.getThreeDBuilder()) == -1)
			for (CompoundProperty p : clusteringData.getFeatures())
				if (p.getCompoundPropertySet().isSensitiveTo3D())
				{
					algs = ArrayUtil.concat(Algorithm.class, algs,
							new Algorithm[] { clusteringData.getThreeDBuilder() });
					break;
				}
		String skipped = "";
		if (clusteringData.isSkippingRedundantFeatures())
			skipped += "skippedRedundantFeatures";
		return CompoundPropertyUtil.getSetMD5(clusteringData.getFeatures(), skipped + getEncodedSettings(algs));
	}

	private static String getEncodedSettings(Algorithm... algs)
	{
		String enc = "";
		if (algs != null)
			for (Algorithm alg : algs)
				if (alg != null)
					enc += PropertyUtil.getPropertyMD5(alg.getProperties(), alg.getName() + enc);
		return enc;
	}

	public String get3DBuilderSDFilePath(ThreeDBuilder builder)
	{
		if (clusteringData != null && clusteringData.getThreeDBuilder() != null
				&& clusteringData.getThreeDBuilder() != builder)
			throw new IllegalStateException("set appropriate builder, already set " + clusteringData.getThreeDBuilder()
					+ " != " + builder);
		return Settings.destinationFile(dataset, getEncodedSettings(builder) + ".3d.sdf");
	}

	public String getFeatureValuesFilePath(CompoundPropertySet cps)
	{
		String enc = (clusteringData != null && cps.isSensitiveTo3D()) ? (getEncodedSettings(clusteringData
				.getThreeDBuilder()) + ".") : "";
		return Settings.destinationFile(dataset, enc + cps.getNameIncludingParams());
	}

	public String getAppDomainValuesFilePath(AppDomainComputer app, String param)
	{
		return Settings.destinationFile(dataset, getEncodedSettingsIncludingFeatures(app) + "." + param);
	}

	public String getSmartsMatchesFilePath(MatchEngine matchEngine, List<ListedFragmentProperty> fragments)
	{
		String allSmartsStrings = "";
		for (ListedFragmentProperty fragment : fragments)
			allSmartsStrings += fragment.getSmarts();
		String enc = StringUtil.getMD5(matchEngine + allSmartsStrings);
		return Settings.destinationFile(dataset, enc + ".matches.csv");
	}

	public String getFeatureTableFilePath(String extension)
	{
		return Settings.destinationFile(dataset, getEncodedSettingsIncludingFeatures() + ".features." + extension);
	}

	public String getClusterAssignmentFilePath()
	{
		if (clusteringData.getDatasetClusterer() == null)
			throw new IllegalStateException();
		return Settings.destinationFile(dataset,
				getEncodedSettingsIncludingFeatures(clusteringData.getDatasetClusterer()) + ".cluster");
	}

	public String getClusterSDFile()
	{
		return getClusterAssignmentFilePath() + ".sdf";
	}

	public String getEmbeddingResultsFilePath(String extension)
	{
		if (clusteringData.getThreeDEmbedder() == null)
			throw new IllegalStateException();
		return Settings.destinationFile(dataset,
				getEncodedSettingsIncludingFeatures(clusteringData.getThreeDEmbedder()) + ".embed." + extension);
	}

	public String getAlignSDFilePath()
	{
		if (clusteringData.getDatasetClusterer() == null || clusteringData.getThreeDAligner() == null)
			throw new IllegalStateException();
		return Settings.destinationFile(
				dataset,
				getEncodedSettingsIncludingFeatures(clusteringData.getDatasetClusterer(),
						clusteringData.getThreeDAligner())
						+ ".align.sdf");
	}

	public String getAlignResultsPerClusterFilePath(int clusterIndex, String params)
	{
		return Settings.destinationFile(
				dataset,
				getEncodedSettingsIncludingFeatures(clusteringData.getDatasetClusterer()) + "."
						+ StringUtil.getMD5(clusterIndex + params) + ".cluster");
	}

}
