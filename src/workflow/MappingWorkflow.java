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
import java.io.FileWriter;
import java.util.Properties;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import main.CheSMapping;
import main.PropHandler;
import main.Settings;
import util.FileUtil;
import alg.align3d.ThreeDAligner;
import alg.build3d.ThreeDBuilder;
import alg.cluster.DatasetClusterer;
import alg.embed3d.ThreeDEmbedder;
import data.DatasetFile;
import dataInterface.MoleculePropertySet;

public class MappingWorkflow
{
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

	public static void exportMappingWorkflowToFile(Properties workflowMappingProps, String outfile)
	{
		try
		{
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

	public static Properties createMappingWorkflow(String datasetFile, String[] featureNames, DatasetClusterer clusterer)
	{
		Properties props = new Properties();

		DatasetWizardPanel datasetProvider = new DatasetWizardPanel();
		datasetProvider.exportDatasetToMappingWorkflow(datasetFile, props);

		FeatureWizardPanel features = new FeatureWizardPanel();
		features.updateIntegratedFeatures(datasetProvider.getDatasetFile());
		features.exportFeaturesToMappingWorkflow(featureNames, props);

		ClusterWizardPanel cluster = new ClusterWizardPanel();
		cluster.exportAlgorithmToMappingWorkflow(clusterer, props);

		return props;
	}

	public static Properties exportSettingsToMappingWorkflow()
	{
		Properties props = new Properties();
		for (MappingWorkflowProvider p : new MappingWorkflowProvider[] { new DatasetWizardPanel(),
				new Build3DWizardPanel(), new FeatureWizardPanel(), new ClusterWizardPanel(), new EmbedWizardPanel(),
				new AlignWizardPanel() })
			p.exportSettingsToMappingWorkflow(props);
		return props;
	}

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
		MoleculePropertySet features[] = f.getFeaturesFromMappingWorkflow(workflowMappingProps, true);
		DatasetClusterer clusterer = (DatasetClusterer) new ClusterWizardPanel().getAlgorithmFromMappingWorkflow(
				workflowMappingProps, true);
		ThreeDEmbedder embedder = (ThreeDEmbedder) new EmbedWizardPanel().getAlgorithmFromMappingWorkflow(
				workflowMappingProps, true);
		ThreeDAligner aligner = (ThreeDAligner) new AlignWizardPanel().getAlgorithmFromMappingWorkflow(
				workflowMappingProps, true);
		PropHandler.storeProperties();
		return new CheSMapping(dataset, features, clusterer, builder, embedder, aligner);
	}

	public static void main(String args[])
	{
		exportMappingWorkflowToFile(createMappingWorkflow("/home/martin/data/caco2.sdf", new String[] { "logD", "rgyr",
				"HCPSA", "fROTB" }, null));
	}
}
