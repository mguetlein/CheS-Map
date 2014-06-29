package workflow;

import gui.AlignWizardPanel;
import gui.Build3DWizardPanel;
import gui.ClusterWizardPanel;
import gui.DatasetWizardPanel;
import gui.EmbedWizardPanel;
import gui.FeatureWizardPanel;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import main.BinHandler;
import main.CheSMapping;
import main.PropHandler;
import main.Settings;
import util.ArrayUtil;
import util.FileUtil;
import alg.align3d.ThreeDAligner;
import alg.build3d.ThreeDBuilder;
import alg.cluster.DatasetClusterer;
import alg.cluster.NoClusterer;
import alg.embed3d.ThreeDEmbedder;
import alg.embed3d.WekaPCA3DEmbedder;
import data.DatasetFile;
import data.cdk.CDKPropertySet;
import data.fminer.FminerPropertySet;
import data.fragments.FragmentProperties;
import data.fragments.ListedFragments;
import data.integrated.IntegratedPropertySet;
import data.obdesc.OBDescriptorSet;
import data.obfingerprints.FingerprintType;
import data.obfingerprints.OBFingerprintSet;
import dataInterface.CompoundProperty;
import dataInterface.CompoundPropertySet;
import dataInterface.CompoundPropertySet.Type;
import dataInterface.FragmentPropertySet;
import dataInterface.NominalProperty;
import dataInterface.NumericProperty;

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

	/**
	 * for selecting descriptor groups as in feature-wizard via string (i.e. command-line)
	 */
	public enum DescriptorCategory
	{
		integrated, cdk, ob, obFP2, obFP3, obFP4, obMACCS, fminer, benigniBossa
	}

	/**
	 * creates hash-map with features (HashMap<String, CompoundPropertySet[]> features) as needed by feature-wizard panel
	 */
	public static class DescriptorSelection
	{
		List<DescriptorCategory> feats;
		String includeIntegrated[];
		String excludeIntegrated[];
		String nominalIntegrated[];
		int fpMinFreq = -1;
		boolean fpSkipOmnipresent = true;

		public DescriptorSelection(String featString)
		{
			this(featString, null, null, null);
		}

		public DescriptorSelection(String featString, String includeIntegrated, String excludeIntegrated,
				String nominalIntegrated)
		{
			feats = new ArrayList<DescriptorCategory>();
			for (String featStr : featString.split(","))
				feats.add(DescriptorCategory.valueOf(featStr));
			if (feats.contains(null) || feats.size() == 0)
				throw new IllegalArgumentException(featString);

			if (includeIntegrated != null)
				this.includeIntegrated = includeIntegrated.split(",");
			if (excludeIntegrated != null)
				this.excludeIntegrated = excludeIntegrated.split(",");
			if (nominalIntegrated != null)
				this.nominalIntegrated = nominalIntegrated.split(",");
		}

		public DescriptorSelection(DescriptorCategory... feats)
		{
			this.feats = ArrayUtil.toList(feats);
		}

		public void setFingerprintSettings(int minFrequency, boolean skipOmnipresent)
		{
			this.fpMinFreq = minFrequency;
			this.fpSkipOmnipresent = skipOmnipresent;
		}

		private CompoundProperty[] filterNotSuited(CompoundProperty[] set, boolean onlyNumeric, String[] include,
				String[] exclude, String[] nominal)
		{
			List<CompoundProperty> feats = new ArrayList<CompoundProperty>();
			for (int i = 0; i < set.length; i++)
				if (!(set[i] instanceof NominalProperty && ((NominalProperty) set[i]).isSmiles()))
					if ((onlyNumeric && set[i] instanceof NumericProperty)
							|| (!onlyNumeric && (set[i].getCompoundPropertySet().isTypeAllowed(Type.NUMERIC) || set[i] instanceof NominalProperty)))
						if (include == null || ArrayUtil.indexOf(include, set[i].getName()) != -1)
							if (exclude == null || ArrayUtil.indexOf(exclude, set[i].getName()) == -1)
								feats.add(set[i]);
			for (CompoundProperty c : set)
				if (nominal != null && ArrayUtil.indexOf(nominal, c.getName()) != -1)
					c.getCompoundPropertySet().setType(Type.NOMINAL);
			return ArrayUtil.toArray(CompoundProperty.class, feats);
		}

		public HashMap<String, CompoundPropertySet[]> getFeatures(DatasetFile dataset)
		{
			HashMap<String, CompoundPropertySet[]> features = new HashMap<String, CompoundPropertySet[]>();

			if (feats.contains(DescriptorCategory.integrated))
			{
				IntegratedPropertySet s[] = dataset.getIntegratedProperties();
				CompoundProperty p[] = new CompoundProperty[s.length];
				for (int i = 0; i < p.length; i++)
					p[i] = s[i].get();
				features.put(
						FeatureWizardPanel.INTEGRATED_FEATURES,
						ArrayUtil.cast(IntegratedPropertySet.class,
								filterNotSuited(p, false, includeIntegrated, excludeIntegrated, nominalIntegrated)));
			}

			if (feats.contains(DescriptorCategory.cdk))
			{
				List<CDKPropertySet> feats = new ArrayList<CDKPropertySet>();
				for (CDKPropertySet p : CDKPropertySet.NUMERIC_DESCRIPTORS)
					if (!p.toString().equals("Ionization Potential"))
						feats.add(p);
				features.put(FeatureWizardPanel.CDK_FEATURES, ArrayUtil.toArray(feats));
			}

			if (feats.contains(DescriptorCategory.obFP2) || feats.contains(DescriptorCategory.obFP3)
					|| feats.contains(DescriptorCategory.obFP4) || feats.contains(DescriptorCategory.obMACCS)
					|| feats.contains(DescriptorCategory.fminer) || feats.contains(DescriptorCategory.benigniBossa))
			{
				if (fpMinFreq == -1)
					fpMinFreq = Math.max(1, Math.min(10, dataset.numCompounds() / 10));
				FragmentProperties.setMinFrequency(fpMinFreq);
				FragmentProperties.setSkipOmniFragments(fpSkipOmnipresent);
				System.out.println("set min frequency to " + FragmentProperties.getMinFrequency());
				Settings.LOGGER.info("before computing structural fragment "
						+ FragmentProperties.getMatchEngine() + " "
						+ FragmentProperties.getMinFrequency() + " "
						+ FragmentProperties.isSkipOmniFragments());
			}

			FragmentPropertySet fps[] = new FragmentPropertySet[0];
			if (feats.contains(DescriptorCategory.obFP2))
				fps = ArrayUtil.concat(FragmentPropertySet.class, fps,
						new OBFingerprintSet[] { OBFingerprintSet.getOBFingerprintSet(FingerprintType.FP2) });
			if (feats.contains(DescriptorCategory.obFP3))
				fps = ArrayUtil.concat(FragmentPropertySet.class, fps,
						new OBFingerprintSet[] { OBFingerprintSet.getOBFingerprintSet(FingerprintType.FP3) });
			if (feats.contains(DescriptorCategory.obFP4))
				fps = ArrayUtil.concat(FragmentPropertySet.class, fps,
						new OBFingerprintSet[] { OBFingerprintSet.getOBFingerprintSet(FingerprintType.FP4) });
			if (feats.contains(DescriptorCategory.obMACCS))
				fps = ArrayUtil.concat(FragmentPropertySet.class, fps,
						new OBFingerprintSet[] { OBFingerprintSet.getOBFingerprintSet(FingerprintType.MACCS) });
			if (feats.contains(DescriptorCategory.benigniBossa))
				fps = ArrayUtil.concat(FragmentPropertySet.class, fps,
						new FragmentPropertySet[] { ListedFragments.instance
								.findFromString(ListedFragments.SMARTS_LIST_PREFIX + "ToxTree_BB_CarcMutRules") });
			if (feats.contains(DescriptorCategory.fminer))
				fps = ArrayUtil.concat(FragmentPropertySet.class, fps,
						new FragmentPropertySet[] { FminerPropertySet.INSTANCE });
			if (fps.length > 0)
				features.put(FeatureWizardPanel.STRUCTURAL_FRAGMENTS, fps);

			if (feats.contains(DescriptorCategory.ob))
				features.put(FeatureWizardPanel.OB_FEATURES, ArrayUtil.cast(
						OBDescriptorSet.class,
						filterNotSuited(OBDescriptorSet.getDescriptorProps(dataset, false), true, null, null,
								null)));
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

		DatasetWizardPanel datasetProvider = new DatasetWizardPanel();
		datasetProvider.exportDatasetToMappingWorkflow(datasetFile, Settings.BIG_DATA, props);
		if (datasetProvider.getDatasetFile() == null)
			throw new IllegalArgumentException("Could not load dataset file: " + datasetFile);

		if (featureSelection != null)
		{
			FeatureWizardPanel features = new FeatureWizardPanel();
			features.updateIntegratedFeatures(datasetProvider.getDatasetFile());
			features.exportFeaturesToMappingWorkflow(featureSelection.getFeatures(datasetProvider.getDatasetFile()),
					props);
		}

		ClusterWizardPanel cluster = new ClusterWizardPanel();
		cluster.exportAlgorithmToMappingWorkflow(clusterer, props);

		EmbedWizardPanel emb = new EmbedWizardPanel();
		emb.exportAlgorithmToMappingWorkflow(embedder, props);

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
		for (MappingWorkflowProvider p : new MappingWorkflowProvider[] { new DatasetWizardPanel(),
				new Build3DWizardPanel(), new FeatureWizardPanel(), new ClusterWizardPanel(), new EmbedWizardPanel(),
				new AlignWizardPanel() })
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
		DatasetFile dataset = new DatasetWizardPanel(true).getDatasetFromMappingWorkflow(workflowMappingProps, true,
				alternateDatasetDir);
		if (dataset == null)
			return null;
		ThreeDBuilder builder = (ThreeDBuilder) new Build3DWizardPanel().getAlgorithmFromMappingWorkflow(
				workflowMappingProps, true);
		FeatureWizardPanel f = new FeatureWizardPanel();
		f.updateIntegratedFeatures(dataset);
		CompoundPropertySet features[] = f.getFeaturesFromMappingWorkflow(workflowMappingProps, true);
		DatasetClusterer clusterer = (DatasetClusterer) new ClusterWizardPanel().getAlgorithmFromMappingWorkflow(
				workflowMappingProps, true);
		ThreeDEmbedder embedder = (ThreeDEmbedder) new EmbedWizardPanel().getAlgorithmFromMappingWorkflow(
				workflowMappingProps, true);
		ThreeDAligner aligner = (ThreeDAligner) new AlignWizardPanel().getAlgorithmFromMappingWorkflow(
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
				DescriptorCategory.integrated), NoClusterer.INSTANCE);
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
		Properties props = MappingWorkflow.createMappingWorkflow(input, new DescriptorSelection(DescriptorCategory.ob),
				null, null);
		CheSMapping mapping = MappingWorkflow.createMappingFromMappingWorkflow(props, "");
		mapping.doMapping();
		//		ClusteringData data = mapping.doMapping();

		//		exportMappingWorkflowToFile(createMappingWorkflow("/home/martin/data/caco2.sdf", new String[] { "logD", "rgyr",
		//				"HCPSA", "fROTB" }, null));
	}

}
