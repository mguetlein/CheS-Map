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
import data.IntegratedProperty;
import data.cdk.CDKPropertySet;
import data.obdesc.OBDescriptorProperty;
import data.obfingerprints.FingerprintType;
import data.obfingerprints.OBFingerprintSet;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;
import dataInterface.CompoundPropertySet;

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
		integrated, cdk, ob, obFP2, obFP3, obFP4, obMACCS;
	}

	/**
	 * creates hash-map with features (HashMap<String, CompoundPropertySet[]> features) as needed by feature-wizard panel
	 */
	public static class DescriptorSelection
	{
		List<DescriptorCategory> feats;
		String excludeIntegrated[];

		public DescriptorSelection(String featString)
		{
			this(featString, null);
		}

		public DescriptorSelection(String featString, String excludeIntegrated)
		{
			feats = new ArrayList<DescriptorCategory>();
			for (String featStr : featString.split(","))
				feats.add(DescriptorCategory.valueOf(featStr));
			if (feats.contains(null) || feats.size() == 0)
				throw new IllegalArgumentException(featString);

			if (excludeIntegrated != null)
				this.excludeIntegrated = excludeIntegrated.split(",");
		}

		public DescriptorSelection(DescriptorCategory... feats)
		{
			this.feats = ArrayUtil.toList(feats);
		}

		private CompoundProperty[] filterNotSuited(CompoundProperty[] set, boolean onlyNumeric, String[] exclude)
		{
			List<CompoundProperty> feats = new ArrayList<CompoundProperty>();
			for (int i = 0; i < set.length; i++)
			{
				if ((onlyNumeric && set[i].getType() == Type.NUMERIC)
						|| (!onlyNumeric && (set[i].isTypeAllowed(Type.NUMERIC) || set[i].getType() == Type.NOMINAL)))
				{
					if (exclude == null || ArrayUtil.indexOf(exclude, set[i].getName()) == -1)
						feats.add(set[i]);
				}
			}
			return ArrayUtil.toArray(CompoundProperty.class, feats);
		}

		public HashMap<String, CompoundPropertySet[]> getFeatures(DatasetFile dataset)
		{
			HashMap<String, CompoundPropertySet[]> features = new HashMap<String, CompoundPropertySet[]>();

			if (feats.contains(DescriptorCategory.integrated))
				features.put(
						FeatureWizardPanel.INTEGRATED_FEATURES,
						ArrayUtil.cast(IntegratedProperty.class,
								filterNotSuited(dataset.getIntegratedProperties(false), false, excludeIntegrated)));

			if (feats.contains(DescriptorCategory.cdk))
			{
				List<CDKPropertySet> feats = new ArrayList<CDKPropertySet>();
				for (CDKPropertySet p : CDKPropertySet.NUMERIC_DESCRIPTORS)
					if (!p.getNameIncludingParams().equals("Ionization Potential"))
						feats.add(p);
				features.put(FeatureWizardPanel.CDK_FEATURES, ArrayUtil.toArray(feats));
			}

			OBFingerprintSet obFP[] = new OBFingerprintSet[0];
			if (feats.contains(DescriptorCategory.obFP2))
				obFP = ArrayUtil.concat(OBFingerprintSet.class, obFP, new OBFingerprintSet[] { new OBFingerprintSet(
						FingerprintType.FP2) });
			if (feats.contains(DescriptorCategory.obFP3))
				obFP = ArrayUtil.concat(OBFingerprintSet.class, obFP, new OBFingerprintSet[] { new OBFingerprintSet(
						FingerprintType.FP3) });
			if (feats.contains(DescriptorCategory.obFP4))
				obFP = ArrayUtil.concat(OBFingerprintSet.class, obFP, new OBFingerprintSet[] { new OBFingerprintSet(
						FingerprintType.FP4) });
			if (feats.contains(DescriptorCategory.obMACCS))
				obFP = ArrayUtil.concat(OBFingerprintSet.class, obFP, new OBFingerprintSet[] { new OBFingerprintSet(
						FingerprintType.MACCS) });
			if (obFP.length > 0)
				features.put(FeatureWizardPanel.STRUCTURAL_FRAGMENTS, obFP);

			if (feats.contains(DescriptorCategory.ob))
				features.put(
						FeatureWizardPanel.OB_FEATURES,
						ArrayUtil.cast(OBDescriptorProperty.class,
								filterNotSuited(OBDescriptorProperty.getDescriptors(true), true, null)));
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
		datasetProvider.exportDatasetToMappingWorkflow(datasetFile, props);
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
		Properties props = createMappingWorkflow(datasetFile, features, clusterer, WekaPCA3DEmbedder.INSTANCE);
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
