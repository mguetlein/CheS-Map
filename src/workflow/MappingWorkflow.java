package workflow;

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

import main.BinHandler;
import main.CheSMapping;
import main.PropHandler;
import main.Settings;
import property.IntegratedPropertySet;
import property.OBDescriptorSet;
import property.PropertySetProvider;
import property.PropertySetProvider.PropertySetShortcut;
import util.ArrayUtil;
import util.FileUtil;
import util.ListUtil;
import alg.align3d.ThreeDAligner;
import alg.build3d.ThreeDBuilder;
import alg.cluster.DatasetClusterer;
import alg.cluster.NoClusterer;
import alg.embed3d.ThreeDEmbedder;
import alg.embed3d.WekaPCA3DEmbedder;
import data.DatasetFile;
import data.fragments.FragmentProperties;
import data.fragments.MatchEngine;
import dataInterface.CompoundPropertySet;
import dataInterface.CompoundPropertySet.Type;

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

	private static List<CompoundPropertySet> setTypeAndFilter(CompoundPropertySet[] set, String[] integratedInclude,
			String[] integratedExclude, String[] integratedSetTypeNumeric, String[] integratedSetTypeNominal)
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
			feats.add(set[i]);
		}
		return feats;
	}

	/**
	 * creates hash-map with features (HashMap<String, CompoundPropertySet[]> features) as needed by feature-wizard panel
	 */
	public static class DescriptorSelection
	{
		List<PropertySetProvider.PropertySetShortcut> feats;
		String includeIntegrated[];
		String excludeIntegrated[];
		String setNumericIntegrated[];
		String setNominalIntegrated[];
		int fpMinFreq = -1;
		boolean fpSkipOmnipresent = true;
		private MatchEngine matchEngine = MatchEngine.OpenBabel;
		List<CompoundPropertySet> features;

		public DescriptorSelection(String featString)
		{
			this(featString, null, null, null, null);
		}

		public DescriptorSelection(String featString, String includeIntegrated, String excludeIntegrated,
				String setNumericIntegrated, String setNominalIntegrated)
		{
			this(parse(featString), includeIntegrated, excludeIntegrated, setNumericIntegrated, setNominalIntegrated);
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

		public DescriptorSelection(PropertySetProvider.PropertySetShortcut... feats)
		{
			this(feats, null, null, null, null);
		}

		public DescriptorSelection(PropertySetProvider.PropertySetShortcut feat, String includeIntegrated,
				String excludeIntegrated, String setNumericIntegrated, String setNominalIntegrated)
		{
			this(new PropertySetShortcut[] { feat }, includeIntegrated, excludeIntegrated, setNumericIntegrated,
					setNominalIntegrated);
		}

		public DescriptorSelection(PropertySetProvider.PropertySetShortcut feats[], String includeIntegrated,
				String excludeIntegrated, String setNumericIntegrated, String setNominalIntegrated)
		{
			if (feats == null || feats.length == 0)
				throw new IllegalArgumentException();

			this.feats = ArrayUtil.toList(feats);
			if (includeIntegrated != null)
				this.includeIntegrated = includeIntegrated.split(",");
			if (excludeIntegrated != null)
				this.excludeIntegrated = excludeIntegrated.split(",");
			if (setNumericIntegrated != null)
				this.setNumericIntegrated = setNumericIntegrated.split(",");
			if (setNominalIntegrated != null)
				this.setNominalIntegrated = setNominalIntegrated.split(",");
		}

		public void setFingerprintSettings(int minFrequency, boolean skipOmnipresent, MatchEngine matchEngine)
		{
			this.fpMinFreq = minFrequency;
			this.fpSkipOmnipresent = skipOmnipresent;
			this.matchEngine = matchEngine;
		}

		public List<CompoundPropertySet> getFeatures(DatasetFile dataset)
		{
			if (features == null)
			{
				if (fpMinFreq == -1)
					fpMinFreq = Math.max(1, Math.min(10, dataset.numCompounds() / 10));
				FragmentProperties.setMinFrequency(fpMinFreq);
				FragmentProperties.setSkipOmniFragments(fpSkipOmnipresent);
				FragmentProperties.setMatchEngine(matchEngine);
				Settings.LOGGER.info("before computing structural fragment " + FragmentProperties.getMatchEngine()
						+ " " + FragmentProperties.getMinFrequency() + " " + FragmentProperties.isSkipOmniFragments());

				CompoundPropertySet featuresArray[] = PropertySetProvider.INSTANCE.getDescriptorSets(dataset,
						ArrayUtil.toArray(feats));
				features = setTypeAndFilter(featuresArray, includeIntegrated, excludeIntegrated, setNumericIntegrated,
						setNominalIntegrated);
				Settings.LOGGER.debug(features.size() + " feature-sets selected for " + ListUtil.toString(feats)
						+ " (unfiltered: " + featuresArray.length + ")");
			}
			return features;
		}
	}

	/**
	 * creates a mapping-workflow that can be stored in a file, or used as input for the wizard
	 * 
	 * @param datasetFile
	 * @param featureSelection
	 * @return
	 */
	public static Properties createMappingWorkflow(String datasetFile, DescriptorSelection featureSelection)
	{
		return createMappingWorkflow(datasetFile, featureSelection, null, WekaPCA3DEmbedder.INSTANCE_NO_PROBS);
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
			DatasetClusterer clusterer, ThreeDEmbedder embedder)
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

			CompoundPropertySet features[] = ArrayUtil.toArray(CompoundPropertySet.class,
					featureSelection.getFeatures(dataset));
			PropertySetProvider.INSTANCE.exportFeaturesToMappingWorkflow(features, props, dataset);
		}
		new ClustererProvider().exportAlgorithmToMappingWorkflow(clusterer, props);
		new EmbedderProvider().exportAlgorithmToMappingWorkflow(embedder, props);

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
	 * creates a workflow using the specified dataset-file and all integrated features
	 * stores the workflow in a file
	 * 
	 * @param datasetFile
	 * @param workflowOutfile
	 */
	public static void createAndStoreMappingWorkflow(String datasetFile, String workflowOutfile)
	{
		createAndStoreMappingWorkflow(datasetFile, workflowOutfile, new DescriptorSelection(
				PropertySetProvider.PropertySetShortcut.integrated), NoClusterer.INSTANCE);
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
			DescriptorSelection features, DatasetClusterer clusterer)
	{
		createAndStoreMappingWorkflow(datasetFile, workflowOutfile, features, clusterer, null);
	}

	public static void createAndStoreMappingWorkflow(String datasetFile, String workflowOutfile,
			DescriptorSelection features, DatasetClusterer clusterer, String additionalExplictProperties)
	{
		Properties props = createMappingWorkflow(datasetFile, features, clusterer, WekaPCA3DEmbedder.INSTANCE);
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
		PropHandler.init(true);
		BinHandler.init();
		//		System.getenv().put("CM_BABEL_PATH", "/home/martin/opentox-ruby/openbabel-2.2.3/bin/babel");

		//		String input = Settings.destinationFile("knime_input.csv");
		String input = "/home/martin/data/caco2.sdf";
		Properties props = MappingWorkflow.createMappingWorkflow(input, new DescriptorSelection(
				PropertySetProvider.PropertySetShortcut.ob), null, null);
		CheSMapping mapping = MappingWorkflow.createMappingFromMappingWorkflow(props, "");
		mapping.doMapping();
		//		ClusteringData data = mapping.doMapping();

		//		exportMappingWorkflowToFile(createMappingWorkflow("/home/martin/data/caco2.sdf", new String[] { "logD", "rgyr",
		//				"HCPSA", "fROTB" }, null));
	}

}
