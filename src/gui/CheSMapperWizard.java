package gui;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import main.CheSMapping;
import main.PropHandler;
import main.ScreenSetup;
import main.Settings;
import util.FileUtil;
import util.ScreenUtil;
import util.SwingUtil;
import workflow.MappingWorkflow;

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
			Settings.LOGGER.error(e1);
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
		setImportActive(true);

		setVisible(true);
	}

	protected void doImport()
	{
		JFileChooser chooser = null;
		String dir = PropHandler.get("workflow-import-dir");
		if (dir == null)
			dir = PropHandler.get("workflow-export-dir");
		if (dir == null)
			dir = System.getProperty("user.home");
		chooser = new JFileChooser(new File(dir));
		chooser.setFileFilter(new FileFilter()
		{
			@Override
			public String getDescription()
			{
				return "CheS-Mapper Wizard Settings File (*.ches)";
			}

			@Override
			public boolean accept(File f)
			{
				return f.isDirectory() || FileUtil.getFilenamExtension(f.getAbsolutePath()).matches("(?i)ches");
			}
		});
		chooser.showOpenDialog(this);
		final File f = chooser.getSelectedFile();
		if (f != null && FileUtil.getFilenamExtension(f.getAbsolutePath()).matches("(?i)ches"))
		{
			PropHandler.put("workflow-import-dir", f.getParent());
			PropHandler.storeProperties();
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
	}

	public static final int RETURN_VALUE_IMPORT = 2;

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
		chesMapping = new CheSMapping(dataset.getDatasetFile(), features.getSelectedFeatures(),
				cluster.getDatasetClusterer(), create3D.get3DBuilder(), embed.get3DEmbedder(), align.getAlginer());
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
