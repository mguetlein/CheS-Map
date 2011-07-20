package gui;

import javax.swing.JFrame;

import main.CheSMapping;
import main.Settings;
import weka.gui.GenericObjectEditor;
import data.ClusteringData;

public class CheSMapperWizard extends WizardDialog
{
	static
	{
		Thread th = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				new GenericObjectEditor();
			}
		});
		th.start();
	}

	DatasetWizardPanel dataset;
	Build3DWizardPanel create3D;
	FeatureSelectionWizardPanel features;
	ClusterWizardPanel cluster;
	EmbedWizardPanel embed;
	AlignWizardPanel align;

	public CheSMapperWizard(JFrame owner)
	{
		this(null, 0);
	}

	public CheSMapperWizard(JFrame owner, int startPanel)
	{
		super(owner, Settings.TITLE + " Wizard (" + Settings.VERSION_STRING + ")", Settings.CHES_MAPPER_IMAGE);
		Settings.TOP_LEVEL_COMPONENT = this;

		setIconImage(Settings.CHES_MAPPER_IMAGE_SMALL.getImage());

		dataset = new DatasetWizardPanel(this);
		create3D = new Build3DWizardPanel(this);
		features = new FeatureSelectionWizardPanel(this);
		cluster = new ClusterWizardPanel(this);
		embed = new EmbedWizardPanel(this);
		align = new AlignWizardPanel(this);

		addPanel(dataset);
		addPanel(create3D);
		addPanel(features);
		addPanel(cluster);
		addPanel(embed);
		addPanel(align);

		getRootPane().setDefaultButton(finish);

		setSize(1024, 768);
		setLocationRelativeTo(owner);

		while (status < startPanel && panels.size() > startPanel)
		{
			if (errorPanel() == -1)
			{
				panels.get(status).proceed();
				update(status + 1);
			}
		}

		setVisible(true);
	}

	protected String getFinishText()
	{
		return "Start mapping";
	}

	protected void update(int status)
	{
		//		if (status == 0 && dataset.getDatasetFile() != null)
		//		{
		if (dataset.getDatasetFile() != null)
		{
			create3D.update(dataset.getDatasetFile());
			features.updateIntegratedFeatures(dataset.getDatasetFile());
		}
		//		}
		//		if (status == 2)
		//		{
		cluster.update(dataset.getDatasetFile(), features.getNumSelectedFeatures());
		embed.update(dataset.getDatasetFile(), features.getNumSelectedFeatures());
		align.update(cluster.getDatasetClusterer());
		//		}
		super.update(status);
	}

	CheSMapping cdw;

	@Override
	public void finish()
	{
		cdw = new CheSMapping(dataset.getDatasetProvider(), features.getFeatureComputer(),
				cluster.getDatasetClusterer(), create3D.get3DBuilder(), embed.get3DEmbedder(), align.getAlginer());
	}

	public boolean isWorkflowSelected()
	{
		return cdw != null;
	}

	public ClusteringData loadDataset(Progressable progressable)
	{
		return cdw.doMapping(progressable);
	}

	public static void main(String args[])
	{
		CheSMapperWizard wwd = new CheSMapperWizard(null);
		wwd.loadDataset(null);
		System.exit(0);
	}
}
