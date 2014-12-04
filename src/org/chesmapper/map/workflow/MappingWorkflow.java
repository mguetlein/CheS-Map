package org.chesmapper.map.workflow;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.chesmapper.map.alg.align3d.NoAligner;
import org.chesmapper.map.alg.align3d.ThreeDAligner;
import org.chesmapper.map.alg.build3d.ThreeDBuilder;
import org.chesmapper.map.alg.cluster.DatasetClusterer;
import org.chesmapper.map.alg.embed3d.ThreeDEmbedder;
import org.chesmapper.map.alg.embed3d.WekaPCA3DEmbedder;
import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.fragments.FragmentProperties;
import org.chesmapper.map.data.fragments.MatchEngine;
import org.chesmapper.map.dataInterface.CompoundPropertySet;
import org.chesmapper.map.dataInterface.CompoundPropertySet.Type;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.CheSMapping;
import org.chesmapper.map.main.PropHandler;
import org.chesmapper.map.main.ScreenSetup;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.property.IntegratedPropertySet;
import org.chesmapper.map.property.OBDescriptorSet;
import org.chesmapper.map.property.PropertySetProvider;
import org.chesmapper.map.property.PropertySetProvider.PropertySetShortcut;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.ListUtil;

public class MappingWorkflow
{
	/**
	 * opens file choose to select destination-file
	 * 
	 * @param workflowMappingProps
	 */
	public static void exportMappingWorkflowToFile(Properties workflowMappingProps)
	{
		String dir = PropHandler.get("workflow-export-dir");
		if (dir == null)
			dir = PropHandler.get("workflow-import-dir");
		if (dir == null)
			dir = System.getProperty("user.home");
		String name = dir + File.separator + "ches-mapper-wizard-settings.ches";
		JFileChooser f = new JFileChooser(dir);
		f.setSelectedFile(new File(name));
		int i = f.showSaveDialog(Settings.TOP_LEVEL_FRAME);
		if (i != JFileChooser.APPROVE_OPTION)
			return;
		String dest = f.getSelectedFile().getAbsolutePath();
		if (!f.getSelectedFile().exists() && !FileUtil.getFilenamExtension(dest).matches("(?i)ches"))
			dest += ".ches";
		if (new File(dest).exists())
		{
			if (JOptionPane
					.showConfirmDialog(Settings.TOP_LEVEL_FRAME, "File '" + dest + "' already exists, overwrite?",
							"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)
				return;
		}
		exportMappingWorkflowToFile(workflowMappingProps, dest);
		PropHandler.put("workflow-export-dir", FileUtil.getParent(dest));
		PropHandler.storeProperties();
	}

	/**
	 * stores the properties in a file
	 * 
	 * @param workflowMappingProps
	 * @param outfile
	 */
	public static void exportMappingWorkflowToFile(Properties workflowMappingProps, String outfile)
	{
		try
		{
			Settings.LOGGER.info("Stored workflow to file: " + outfile);
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(outfile)));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BufferedOutputStream out = new BufferedOutputStream(baos);
			workflowMappingProps.store(out, "---No Comment---");
			out.close();
			baos.close();
			String pStr = baos.toString();
			writer.write(pStr);
			writer.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static DatasetClusterer clustererFromName(String clusterer)
	{
		for (DatasetClusterer a : DatasetClusterer.CLUSTERERS)
			if (a.getName().equals(clusterer))
				return a;
		if (clusterer != null && clusterer.length() > 0)
			throw new IllegalArgumentException("Dataset clusterer not found: " + clusterer);
		return null;
	}

	private static List<CompoundPropertySet> setTypeAndFilter(CompoundPropertySet[] set, boolean autoSelectIntegrated,
			String[] integratedInclude, String[] integratedExclude, String[] integratedSetTypeNumeric,
			String[] integratedSetTypeNominal)
	{
		List<CompoundPropertySet> feats = new ArrayList<CompoundPropertySet>();
		for (int i = 0; i < set.length; i++)
		{
			if (set[i].isSmiles())
				continue;
			if (set[i] instanceof OBDescriptorSet && set[i].getType() != Type.NUMERIC)
				continue;
			if (set[i].getType() == null && set[i].isTypeAllowed(Type.NUMERIC))
				throw new IllegalStateException("type should have been set beforehand");

			if (set[i] instanceof IntegratedPropertySet)
			{
				if (autoSelectIntegrated)
				{
					if (set[i].getType() == null)
						continue;
				}
				else
				{
					if (integratedInclude != null && ArrayUtil.indexOf(integratedInclude, set[i].toString()) == -1)
						continue;
					if (integratedExclude != null && ArrayUtil.indexOf(integratedExclude, set[i].toString()) != -1)
						continue;
					if (integratedSetTypeNumeric != null
							&& ArrayUtil.indexOf(integratedSetTypeNumeric, set[i].toString()) != -1)
						set[i].setType(Type.NUMERIC);
					if (integratedSetTypeNominal != null
							&& ArrayUtil.indexOf(integratedSetTypeNominal, set[i].toString()) != -1)
						set[i].setType(Type.NOMINAL);
					if (set[i].getType() == null)
						throw new IllegalStateException("integrated feature '" + set[i]
								+ "' is probably not suited for embedding, skip it or set type manually");
				}
			}
			feats.add(set[i]);
		}
		return feats;
	}

	public static class FragmentSettings
	{
		private int minFreq = -1;
		private boolean skipOmnipresent = true;
		private MatchEngine matchEngine = MatchEngine.OpenBabel;

		public FragmentSettings(int minFreq, boolean skipOmnipresent, MatchEngine matchEngine)
		{
			this.minFreq = minFreq;
			this.skipOmnipresent = skipOmnipresent;
			this.matchEngine = matchEngine;
		}

		public int getMinFreq()
		{
			return minFreq;
		}

		public boolean isSkipOmnipresent()
		{
			return skipOmnipresent;
		}

		public MatchEngine getMatchEngine()
		{
			return matchEngine;
		}

		public void apply(DatasetFile dataset)
		{
			if (minFreq == -1)
				minFreq = Math.max(1, Math.min(10, dataset.numCompounds() / 10));
			FragmentProperties.setMinFrequency(minFreq);
			FragmentProperties.setSkipOmniFragments(skipOmnipresent);
			FragmentProperties.setMatchEngine(matchEngine);
			Settings.LOGGER.info("before computing structural fragment " + FragmentProperties.getMatchEngine() + " "
					+ FragmentProperties.getMinFrequency() + " " + FragmentProperties.isSkipOmniFragments());
		}
	}

	/**
	 * creates hash-map with features (HashMap<String, CompoundPropertySet[]> features) as needed by feature-wizard panel
	 */
	public static class DescriptorSelection
	{
		List<PropertySetProvider.PropertySetShortcut> feats;

		boolean autoSelectIntegrated;
		String includeIntegrated[];
		String excludeIntegrated[];
		String setNumericIntegrated[];
		String setNominalIntegrated[];

		List<CompoundPropertySet> features;

		public static DescriptorSelection select(String featString, String includeIntegrated, String excludeIntegrated,
				String setNumericIntegrated, String setNominalIntegrated)
		{
			return new DescriptorSelection(parse(featString), null, includeIntegrated, excludeIntegrated,
					setNumericIntegrated, setNominalIntegrated);
		}

		public static DescriptorSelection select(PropertySetShortcut feat, String includeIntegrated,
				String excludeIntegrated, String setNumericIntegrated, String setNominalIntegrated)
		{
			return new DescriptorSelection(new PropertySetShortcut[] { feat }, null, includeIntegrated,
					excludeIntegrated, setNumericIntegrated, setNominalIntegrated);
		}

		public static DescriptorSelection autoSelectIntegrated()
		{
			return new DescriptorSelection(new PropertySetShortcut[] { PropertySetShortcut.integrated }, true, null,
					null, null, null);
		}

		private static PropertySetShortcut[] parse(String s)
		{
			List<PropertySetShortcut> feats = new ArrayList<PropertySetShortcut>();
			for (String featStr : s.split(","))
				feats.add(PropertySetProvider.PropertySetShortcut.valueOf(featStr));
			if (feats.contains(null) || feats.size() == 0)
				throw new IllegalArgumentException(s);
			return ListUtil.toArray(feats);
		}

		private DescriptorSelection(PropertySetProvider.PropertySetShortcut feats[], Boolean autoSelectIntegrated,
				String includeIntegrated, String excludeIntegrated, String setNumericIntegrated,
				String setNominalIntegrated)
		{
			if (feats == null || feats.length == 0)
				throw new IllegalArgumentException();

			this.feats = ArrayUtil.toList(feats);

			if (autoSelectIntegrated != null)
				this.autoSelectIntegrated = autoSelectIntegrated;
			else
			{
				if (includeIntegrated != null)
					this.includeIntegrated = includeIntegrated.split(",");
				if (excludeIntegrated != null)
					this.excludeIntegrated = excludeIntegrated.split(",");
				if (setNumericIntegrated != null)
					this.setNumericIntegrated = setNumericIntegrated.split(",");
				if (setNominalIntegrated != null)
					this.setNominalIntegrated = setNominalIntegrated.split(",");
			}
		}

		public List<CompoundPropertySet> getFilteredFeatures(DatasetFile dataset)
		{
			CompoundPropertySet featuresArray[] = PropertySetProvider.INSTANCE.getDescriptorSets(dataset,
					ArrayUtil.toArray(feats));
			features = setTypeAndFilter(featuresArray, autoSelectIntegrated, includeIntegrated, excludeIntegrated,
					setNumericIntegrated, setNominalIntegrated);
			Settings.LOGGER.debug(features.size() + " feature-sets selected for " + ListUtil.toString(feats)
					+ " (unfiltered: " + featuresArray.length + ")");
			return features;
		}

		public List<PropertySetProvider.PropertySetShortcut> getShortcuts()
		{
			return feats;
		}
	}

	/**
	 * creates a mapping-workflow that can be stored in a file, or used as input for the wizard
	 * 
	 * @param datasetFile
	 * @param featureSelection
	 * @return
	 */
	public static Properties createMappingWorkflow(String datasetFile, DescriptorSelection featureSelection,
			FragmentSettings fragmentSettings)
	{
		return createMappingWorkflow(datasetFile, featureSelection, fragmentSettings, null,
				WekaPCA3DEmbedder.INSTANCE_NO_PROBS);
	}

	/**
	 * creates a mapping-workflow that can be stored in a file, or used as input for the wizard
	 * 
	 * @param datasetFile
	 * @param featureNames
	 * @param selectAllInternalFeatures
	 * @param clusterer
	 * @return
	 */
	public static Properties createMappingWorkflow(String datasetFile, DescriptorSelection featureSelection,
			FragmentSettings fragmentSettings, DatasetClusterer clusterer, ThreeDEmbedder embedder)
	{
		return createMappingWorkflow(datasetFile, featureSelection, fragmentSettings, clusterer, embedder,
				NoAligner.INSTANCE);
	}

	public static Properties createMappingWorkflow(String datasetFile, DescriptorSelection featureSelection,
			FragmentSettings fragmentSettings, DatasetClusterer clusterer, ThreeDEmbedder embedder,
			ThreeDAligner aligner)
	{
		Properties props = new Properties();

		DatasetMappingWorkflowProvider datasetProvider = new DatasetLoader(false);
		DatasetFile dataset = datasetProvider.exportDatasetToMappingWorkflow(datasetFile, Settings.BIG_DATA, props);
		if (dataset == null)
			throw new IllegalArgumentException("Could not load dataset file: " + datasetFile);

		if (featureSelection != null)
		{
			//			FeatureWizardPanel features = new FeatureWizardPanel();
			//			features.updateFeatures(dataset);
			//			features.exportFeaturesToMappingWorkflow(featureSelection.getFeatures(datasetProvider.getDatasetFile()),
			//					props);

			if (fragmentSettings != null)
				fragmentSettings.apply(dataset);
			CompoundPropertySet features[] = ArrayUtil.toArray(CompoundPropertySet.class,
					featureSelection.getFilteredFeatures(dataset));
			PropertySetProvider.INSTANCE.exportFeaturesToMappingWorkflow(features, props, dataset);
		}
		new ClustererProvider().exportAlgorithmToMappingWorkflow(clusterer, props);
		new EmbedderProvider().exportAlgorithmToMappingWorkflow(embedder, props);
		new AlignerProvider().exportAlgorithmToMappingWorkflow(aligner, props);

		return props;
	}

	/**
	 * extracts a mapping-workflow from the current-settings 
	 * 
	 * @return
	 */
	public static Properties exportSettingsToMappingWorkflow()
	{
		Properties props = new Properties();
		for (MappingWorkflowProvider p : new MappingWorkflowProvider[] { new DatasetLoader(false),
				new BuilderProvider(), PropertySetProvider.INSTANCE, new ClustererProvider(), new EmbedderProvider(),
				new AlignerProvider() })
			p.exportSettingsToMappingWorkflow(props);
		return props;
	}

	/**
	 * loads the worklow from a file, and creates a mapping (resulst from ches-mapper-wizard)
	 * can be used to directly start the viewer
	 * this stores the mapping-workflow in the ches-mapp-prop-file
	 * 
	 * @param workflowFile
	 * @return
	 */
	public static CheSMapping createMappingFromMappingWorkflow(String workflowFile)
	{
		try
		{
			File f = new File(workflowFile);
			Properties props = new Properties();
			FileInputStream in = new FileInputStream(f);
			props.load(in);
			in.close();
			return createMappingFromMappingWorkflow(props, f.getParent());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * creates a mapping (result form ches-mapper-wizard) from properties
	 * can be used to directly start the viewer
	 * this stores the mapping-workflow in the ches-mapp-prop-file
	 * 
	 * @param workflowMappingProps
	 * @return
	 */
	public static CheSMapping createMappingFromMappingWorkflow(Properties workflowMappingProps)
	{
		return createMappingFromMappingWorkflow(workflowMappingProps, System.getProperty("user.home"));
	}

	/**
	 * creates a mapping (result form ches-mapper-wizard) from properties
	 * can be used to directly start the viewer
	 * this stores the mapping-workflow in the ches-mapp-prop-file
	 * 
	 * @param workflowMappingProps
	 * @param alternateDatasetDir
	 * @return
	 */
	public static CheSMapping createMappingFromMappingWorkflow(Properties workflowMappingProps,
			String alternateDatasetDir)
	{
		DatasetFile dataset = new DatasetLoader(Settings.TOP_LEVEL_FRAME != null).getDatasetFromMappingWorkflow(
				workflowMappingProps, true, alternateDatasetDir);
		if (dataset == null)
			return null;
		ThreeDBuilder builder = (ThreeDBuilder) new BuilderProvider().getAlgorithmFromMappingWorkflow(
				workflowMappingProps, true);
		//		FeatureWizardPanel f = new FeatureWizardPanel();
		//		f.updateFeatures(dataset);
		CompoundPropertySet features[] = PropertySetProvider.INSTANCE.getFeaturesFromMappingWorkflow(
				workflowMappingProps, true, dataset);
		DatasetClusterer clusterer = (DatasetClusterer) new ClustererProvider().getAlgorithmFromMappingWorkflow(
				workflowMappingProps, true);
		ThreeDEmbedder embedder = (ThreeDEmbedder) new EmbedderProvider().getAlgorithmFromMappingWorkflow(
				workflowMappingProps, true);
		ThreeDAligner aligner = (ThreeDAligner) new AlignerProvider().getAlgorithmFromMappingWorkflow(
				workflowMappingProps, true);
		PropHandler.storeProperties();
		return new CheSMapping(dataset, features, clusterer, builder, embedder, aligner);
	}

	/**
	 * creates a workflow using the specified dataset-file and the selected features
	 * stores the workflow in a file
	 * 
	 * @param datasetFile
	 * @param workflowOutfile
	 * @param ignoreIntegratedFeatures
	 */
	public static void createAndStoreMappingWorkflow(String datasetFile, String workflowOutfile,
			DescriptorSelection features, FragmentSettings fragmentSettings, DatasetClusterer clusterer)
	{
		createAndStoreMappingWorkflow(datasetFile, workflowOutfile, features, fragmentSettings, clusterer, null);
	}

	public static void createAndStoreMappingWorkflow(String datasetFile, String workflowOutfile,
			DescriptorSelection features, FragmentSettings fragmentSettings, DatasetClusterer clusterer,
			String additionalExplictProperties)
	{
		Properties props = createMappingWorkflow(datasetFile, features, fragmentSettings, clusterer,
				WekaPCA3DEmbedder.INSTANCE);
		if (additionalExplictProperties != null)
		{
			for (String p : additionalExplictProperties.split(","))
			{
				String keyValue[] = p.split("=");
				if (keyValue.length != 2)
					throw new IllegalArgumentException(p);
				props.setProperty(keyValue[0], keyValue[1]);
			}
		}
		MappingWorkflow.exportMappingWorkflowToFile(props, workflowOutfile);
	}

	public static void main(String args[])
	{
		ScreenSetup.INSTANCE = ScreenSetup.DEFAULT;
		PropHandler.init(true);
		BinHandler.init();
		//		System.getenv().put("CM_BABEL_PATH", "/home/martin/opentox-ruby/openbabel-2.2.3/bin/babel");

		//		String input = Settings.destinationFile("knime_input.csv");
		String input = "/home/martin/data/caco2/caco2.sdf";
		Properties props = MappingWorkflow.createMappingWorkflow(input, DescriptorSelection.autoSelectIntegrated(),
				null);
		CheSMapping mapping = MappingWorkflow.createMappingFromMappingWorkflow(props, "");
		mapping.doMapping();
		//		ClusteringData data = mapping.doMapping();

		//		exportMappingWorkflowToFile(createMappingWorkflow("/home/martin/data/caco2.sdf", new String[] { "logD", "rgyr",
		//				"HCPSA", "fROTB" }, null));
	}

}
