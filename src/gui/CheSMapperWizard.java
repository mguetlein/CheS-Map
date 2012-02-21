package gui;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JFrame;

import main.CheSMapping;
import main.Settings;
import util.SwingUtil;
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
	FeatureWizardPanel features;
	ClusterWizardPanel cluster;
	EmbedWizardPanel embed;
	AlignWizardPanel align;

	CheSMapping chesMapping;

	public CheSMapperWizard(JFrame owner)
	{
		this(null, 0);
	}

	public CheSMapperWizard(JFrame owner, int startPanel)
	{
		super(owner, Settings.TITLE + " Wizard (" + Settings.VERSION_STRING + ")", Settings.CHES_MAPPER_IMAGE,
				Settings.OPENTOX_ICON, Settings.HOMEPAGE_DOCUMENTATION);
		addClickLinkToIcon(Settings.HOMEPAGE);
		addClickLinkToAdditionalIcon("http://opentox.org");

		Settings.TOP_LEVEL_FRAME = this;

		setIconImage(Settings.CHES_MAPPER_IMAGE_SMALL.getImage());

		dataset = new DatasetWizardPanel(this);
		create3D = new Build3DWizardPanel(this);
		features = new FeatureWizardPanel(this);
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

		Dimension d = new Dimension(1024, 768);
		Dimension full = Settings.SCREEN_SETUP.getFullScreenSize();
		d.width = Math.min(full.width - 100, d.width);
		d.height = Math.min(full.height - 100, d.height);
		setSize(d);
		if (owner != null)
			setLocationRelativeTo(owner);
		else
			Settings.SCREEN_SETUP.centerOnScreen(this);

		while (status < startPanel && panels.size() > startPanel)
		{
			if (errorPanel() == -1)
			{
				panels.get(status).proceed();
				update(status + 1);
			}
		}

		((JComponent) getContentPane().getComponent(0)).setBorder(Settings.SCREEN_SETUP.getWizardBorder());

		setVisible(true);
	}

	protected String getFinishText()
	{
		return "Start mapping";
	}

	protected void update(int status)
	{
		if (dataset.getDatasetFile() != null)
		{
			create3D.update(dataset.getDatasetFile(), null, null);
			features.updateIntegratedFeatures(dataset.getDatasetFile());
			if (features.getFeatureInfo() != null)
			{
				cluster.update(dataset.getDatasetFile(), features.getFeatureInfo(), cluster.getDatasetClusterer());
				embed.update(dataset.getDatasetFile(), features.getFeatureInfo(), cluster.getDatasetClusterer());
				align.update(dataset.getDatasetFile(), features.getFeatureInfo(), cluster.getDatasetClusterer());
			}
		}
		super.update(status);
	}

	@Override
	public void finish()
	{
		chesMapping = new CheSMapping(dataset.getDatasetProvider(), features.getFeatureComputer(),
				cluster.getDatasetClusterer(), create3D.get3DBuilder(), embed.get3DEmbedder(), align.getAlginer());
	}

	public boolean isWorkflowSelected()
	{
		return chesMapping != null;
	}

	public ClusteringData loadDataset()
	{
		return chesMapping.doMapping();
	}

	public static void main(String args[])
	{
		CheSMapperWizard wwd = new CheSMapperWizard(null);
		SwingUtil.waitWhileVisible(wwd);
		wwd.loadDataset();
		System.exit(0);
	}
}
