package org.chesmapper.map.gui;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.chesmapper.map.main.CheSMapping;
import org.chesmapper.map.main.ScreenSetup;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.workflow.MappingWorkflow;
import org.mg.javalib.gui.WizardDialog;
import org.mg.javalib.util.ScreenUtil;
import org.mg.javalib.util.SwingUtil;

public class CheSMapperWizard extends WizardDialog
{
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

		setSize(ScreenSetup.INSTANCE.getWizardSize());
		if (owner != null && owner.getRootPane().isShowing())
			setLocationRelativeTo(owner);
		else
			ScreenSetup.INSTANCE.centerOnScreen(this);

		try
		{
			if (ScreenSetup.INSTANCE.isWizardUndecorated())
				setUndecorated(ScreenSetup.INSTANCE.isWizardUndecorated());
		}
		catch (Exception e1)
		{
			Settings.LOGGER.error(e1);
		}

		addComponentListener(new ComponentAdapter()
		{
			public void componentMoved(ComponentEvent e)
			{
				Settings.TOP_LEVEL_FRAME_SCREEN = ScreenUtil.getScreen(CheSMapperWizard.this);
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

		((JComponent) getContentPane().getComponent(0)).setBorder(ScreenSetup.INSTANCE.getWizardBorder());
		setImportActive(false);

		setVisible(true);
	}

	public void initImport(final File f)
	{
		Thread th = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							CheSMapperWizard.this.block("create worflow");
						}
					});
					// do not do this in AWT event thread, this will cause errors
					chesMapping = MappingWorkflow.createMappingFromMappingWorkflow(f.getAbsolutePath());
				}
				finally
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							CheSMapperWizard.this.unblock("create worflow");
						}
					});
				}
				if (chesMapping != null)
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							close(RETURN_VALUE_IMPORT);
						}
					});
			}

		});
		th.start();
	}

	public static final int RETURN_VALUE_IMPORT = 2;

	protected String getFinishText()
	{
		return "Start mapping";
	}

	protected void update(int status)
	{
		if (isClosed())
			return;
		if (dataset.getDatasetFile() != null)
		{
			create3D.update(dataset.getDatasetFile(), null, null);
			features.updateFeatures(dataset.getDatasetFile());
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
		chesMapping = new CheSMapping(dataset.getDatasetFile(), features.getSelectedFeatures(),
				cluster.getDatasetClusterer(), create3D.get3DBuilder(), embed.get3DEmbedder(), align.getAlginer());
	}

	private boolean closed = false;

	public boolean isClosed()
	{
		return closed;
	}

	protected void close(int returnValue)
	{
		closed = true;
		super.close(returnValue);
	}

	public CheSMapping getChesMapping()
	{
		return chesMapping;
	}

	public static void main(String args[])
	{
		CheSMapperWizard wwd = new CheSMapperWizard(null);
		SwingUtil.waitWhileVisible(wwd);
		wwd.getChesMapping().doMapping();
		System.exit(0);
	}
}
