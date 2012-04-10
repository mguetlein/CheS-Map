package gui;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;

import main.CheSMapping;
import main.ScreenSetup;
import main.Settings;
import util.ScreenUtil;
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
				Settings.OPENTOX_IMAGE, Settings.HOMEPAGE_DOCUMENTATION);
		addClickLinkToIcon(Settings.HOMEPAGE);
		addClickLinkToAdditionalIcon("http://opentox.org");

		Settings.TOP_LEVEL_FRAME = this;

		setIconImage(Settings.CHES_MAPPER_ICON.getImage());

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

		getRootPane().setDefaultButton(buttonFinish);

		setSize(ScreenSetup.SETUP.getWizardSize());
		if (owner != null && owner.getRootPane().isShowing())
			setLocationRelativeTo(owner);
		else
			ScreenSetup.SETUP.centerOnScreen(this);

		try
		{
			if (ScreenSetup.SETUP.isWizardUndecorated())
				setUndecorated(ScreenSetup.SETUP.isWizardUndecorated());
		}
		catch (Exception e1)
		{
			e1.printStackTrace();
		}

		addComponentListener(new ComponentAdapter()
		{
			public void componentMoved(ComponentEvent e)
			{
				ScreenSetup.SETUP.setScreen(ScreenUtil.getScreen(CheSMapperWizard.this));
			}
		});

		while (status < startPanel && panels.size() > startPanel)
		{
			if (errorPanel() == -1)
			{
				panels.get(status).proceed();
				update(status + 1);
			}
		}

		((JComponent) getContentPane().getComponent(0)).setBorder(ScreenSetup.SETUP.getWizardBorder());

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

	public ClusteringData doMapping()
	{
		return chesMapping.doMapping();
	}

	public static void main(String args[])
	{
		CheSMapperWizard wwd = new CheSMapperWizard(null);
		SwingUtil.waitWhileVisible(wwd);
		wwd.doMapping();
		System.exit(0);
	}
}
