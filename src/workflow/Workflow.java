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

public class Workflow
{
	public static void exportWorkflowToFile(Properties workflow)
	{
		String dir = PropHandler.get("workflow-export-dir");
		if (dir == null)
			dir = PropHandler.get("workflow-import-dir");
		if (dir == null)
			dir = System.getProperty("user.home");
		String name = dir + File.separator + "ches-mapper-workflow.ches";
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
		try
		{
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dest)));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			BufferedOutputStream out = new BufferedOutputStream(baos);
			workflow.store(out, "---No Comment---");
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
		PropHandler.put("workflow-export-dir", FileUtil.getParent(dest));
		PropHandler.storeProperties();
	}

	public static Properties createWorkflow(String file, String[] featureNames, DatasetClusterer clusterer)
	{
		Properties props = new Properties();

		DatasetWizardPanel datasetProvider = new DatasetWizardPanel();
		datasetProvider.exportDatasetToWorkflow(file, props);

		FeatureWizardPanel features = new FeatureWizardPanel();
		features.updateIntegratedFeatures(datasetProvider.getDatasetFile());
		features.exportFeaturesToWorkflow(featureNames, props);

		ClusterWizardPanel cluster = new ClusterWizardPanel();
		cluster.exportAlgorithmToWorkflow(clusterer, props);

		return props;
	}

	public static Properties exportWorkflow()
	{
		Properties props = new Properties();
		for (WorkflowProvider p : new WorkflowProvider[] { new DatasetWizardPanel(), new Build3DWizardPanel(),
				new FeatureWizardPanel(), new ClusterWizardPanel(), new EmbedWizardPanel(), new AlignWizardPanel() })
			p.exportSettingsToWorkflow(props);
		return props;
	}

	public static CheSMapping createMappingFromWorkflow(Properties props, String alternateDatasetDir)
	{
		DatasetFile dataset = new DatasetWizardPanel().getDatasetFromWorkflow(props, true, alternateDatasetDir);
		if (dataset == null)
			return null;
		ThreeDBuilder builder = (ThreeDBuilder) new Build3DWizardPanel().getAlgorithmFromWorkflow(props, true);
		FeatureWizardPanel f = new FeatureWizardPanel();
		f.updateIntegratedFeatures(dataset);
		MoleculePropertySet features[] = f.getFeaturesFromWorkflow(props, true);
		DatasetClusterer clusterer = (DatasetClusterer) new ClusterWizardPanel().getAlgorithmFromWorkflow(props, true);
		ThreeDEmbedder embedder = (ThreeDEmbedder) new EmbedWizardPanel().getAlgorithmFromWorkflow(props, true);
		ThreeDAligner aligner = (ThreeDAligner) new AlignWizardPanel().getAlgorithmFromWorkflow(props, true);
		PropHandler.storeProperties();
		return new CheSMapping(dataset, features, clusterer, builder, embedder, aligner);
	}

	public static void main(String args[])
	{
		exportWorkflowToFile(createWorkflow("/home/martin/data/caco2.sdf", new String[] { "logD", "rgyr", "HCPSA",
				"fROTB" }, null));
	}
}
