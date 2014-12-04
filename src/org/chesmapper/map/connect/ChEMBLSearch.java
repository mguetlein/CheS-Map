package org.chesmapper.map.connect;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.chesmapper.map.gui.DatasetWizardPanel;
import org.chesmapper.map.main.PropHandler;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.mg.javalib.gui.TextPanel;
import org.mg.javalib.gui.property.IntegerProperty;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.gui.property.PropertyPanel;
import org.mg.javalib.gui.property.StringProperty;
import org.mg.javalib.task.Task;
import org.mg.javalib.task.TaskDialog;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.StringUtil;
import org.mg.javalib.util.SwingUtil;
import org.mg.javalib.util.ThreadUtil;
import org.mg.javalib.util.FileUtil.CSVFile;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import uk.ac.ebi.chemblws.domain.Compound;
import uk.ac.ebi.chemblws.restclient.ChemblRestClient;

import com.jgoodies.forms.factories.ButtonBarFactory;

public class ChEMBLSearch
{
	public static String[] getHeader()
	{
		return new String[] { "SMILES", "ChEMBLId", "Similarity", "AcdAcidicPka", "AcdBasicPka", "AcdLogd", "AcdLogp",
				"Alogp", "KnownDrug", "MedChemFriendly", "MolecularFormula", "MolecularWeight", "NumRo5Violations",
				"PassesRuleOfThree", "PreferredCompoundName", "RotatableBonds", "Species", "StdInChiKey", "Synonyms" };
	}

	public static String[] getInfo(Compound c)
	{
		return ArrayUtil.toStringArray(new Object[] { c.getSmiles(), c.getChemblId(), c.getSimilarity(),
				c.getAcdAcidicPka(), c.getAcdBasicPka(), c.getAcdLogd(), c.getAcdLogp(), c.getAlogp(),
				c.getKnownDrug(), c.getMedChemFriendly(), c.getMolecularFormula(), c.getMolecularWeight(),
				c.getNumRo5Violations(), c.getPassesRuleOfThree(), c.getPreferredCompoundName(), c.getRotatableBonds(),
				c.getSpecies(), c.getStdInChiKey(), c.getSynonyms() });
	}

	public static void toCSV(String outfile, List<Compound> compounds)
	{
		List<String[]> list = new ArrayList<String[]>();
		list.add(getHeader());
		for (Compound compound : compounds)
			list.add(getInfo(compound));
		CSVFile csv = new CSVFile();
		csv.content = list;
		FileUtil.writeCSV(outfile, csv, false);
	}

	ApplicationContext applicationContext = new ClassPathXmlApplicationContext("applicationContext.xml");
	ChemblRestClient chemblClient = applicationContext.getBean("chemblRestClient", ChemblRestClient.class);
	List<Compound> similarCompounds;
	JFrame owner;

	public void searchSimularBySmiles(final String smiles, final int simScore)
	{
		Settings.LOGGER.debug("ChEMBLSearch> searching simular for smiles " + smiles + " " + simScore);
		similarCompounds = chemblClient.getSimilarCompoundBySmiles(smiles, simScore);
	}

	public void searchBySubstructure(final String smiles) throws Exception
	{
		Settings.LOGGER.debug("ChEMBLSearch> searching by substructure " + smiles);
		similarCompounds = chemblClient.getCompoundBySubstructureSmiles(smiles);
	}

	private static StringProperty smiles = new StringProperty("Smiles for similarity search",
			"COc1ccc2[C@@H]3[C@H](COc2c1)C(C)(C)OC4=C3C(=O)C(=O)C5=C4OC(C)(C)[C@@H]6COc7cc(OC)ccc7[C@H]56");
	private static IntegerProperty sim = new IntegerProperty("Similarity index", 80);

	private static StringProperty smiles2 = new StringProperty("Smiles for substructure search", "CNN=O");

	private static ChEMBLSearch cs;

	public static void searchDialog(final JFrame parent, final String outfileBasename,
			final DatasetWizardPanel datasetWizardPanel, boolean storeProperties)
	{
		if (cs == null)
		{
			Thread th = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					cs = new ChEMBLSearch();
				}
			});
			th.start();
		}

		PropertyPanel panel = new PropertyPanel(new Property[] { smiles, sim },
				storeProperties ? PropHandler.getProperties() : null, storeProperties ? PropHandler.getPropertiesFile()
						: null);
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));

		PropertyPanel panel2 = new PropertyPanel(new Property[] { smiles2 },
				storeProperties ? PropHandler.getProperties() : null, storeProperties ? PropHandler.getPropertiesFile()
						: null);
		panel2.setBorder(new EmptyBorder(5, 5, 5, 5));

		final JTabbedPane tab = new JTabbedPane();
		tab.addTab("Search by smiles similarity", panel);
		tab.addTab("Search by substructure", panel2);

		final JDialog d = new JDialog(parent, "Search ChEMBL Database");
		if (parent != null)
			d.setModal(true);
		final JButton ok = new JButton("Start search");
		JButton cancel = new JButton("Cancel");
		final StringBuffer okPressed = new StringBuffer("");
		ActionListener al = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (e.getSource() == ok)
					okPressed.append("true");
				d.setVisible(false);
			}
		};
		ok.addActionListener(al);
		cancel.addActionListener(al);
		JPanel p = new JPanel(new BorderLayout(5, 5));
		JPanel pp = new JPanel(new BorderLayout(5, 5));
		pp.add(tab, BorderLayout.CENTER);
		pp.add(new TextPanel(
				"The search uses the ChEMBL Web Services as documented here: https://www.ebi.ac.uk/chembldb/index.php/ws"),
				BorderLayout.SOUTH);
		p.add(pp, BorderLayout.CENTER);
		p.add(ButtonBarFactory.buildOKCancelBar(ok, cancel), BorderLayout.SOUTH);
		p.setBorder(new EmptyBorder(10, 10, 10, 10));
		d.getContentPane().add(p);
		d.pack();
		d.pack();
		if (d.getWidth() < 500)
			d.setSize(500, d.getHeight());
		d.setLocationRelativeTo(parent);
		d.setVisible(true);
		if (!d.isModal())
			SwingUtil.waitWhileVisible(d);
		if (okPressed.toString().equals("true"))
		{
			panel.store();
			panel2.store();
			Thread th = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					final Task task = TaskProvider.initTask("Waiting for ChEMBL");
					new TaskDialog(task, parent);
					task.update("Init ChEMBL REST client");

					while (cs == null)
						ThreadUtil.sleep(100);

					Thread thr = new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								if (tab.getSelectedIndex() == 0)
									cs.searchSimularBySmiles(smiles.getValue(), sim.getValue());
								else
									cs.searchBySubstructure(smiles2.getValue());
							}
							catch (Exception e)
							{
								Settings.LOGGER.error(e);
								if (tab.getSelectedIndex() == 0 && sim.getValue() < 70)
									task.failed("Search failed: " + e.getMessage(),
											"The similarity value should be >= 70");
								else
									task.failed("Search failed", e);
							}
						}
					});
					thr.start();

					if (task.isRunning())
						task.update("Waiting for search result");
					while (cs.similarCompounds == null)
					{
						if (!task.isRunning())
							break;
						ThreadUtil.sleep(100);
					}
					if (cs.similarCompounds != null)
					{
						Settings.LOGGER.debug("ChEMBLSearch> found " + cs.similarCompounds.size() + " compounds");
						if (cs.similarCompounds.size() == 0)
							task.failed("No compounds found", "");
						else
						{
							String outf = null;
							if (outfileBasename != null)
							{
								String suffix;
								if (tab.getSelectedIndex() == 0)
									suffix = StringUtil.getMD5("simular-smiles" + smiles.getValue() + sim.getValue()
											+ (parent != null ? parent.hashCode() : ""));
								else
									suffix = StringUtil.getMD5("substructure" + smiles2.getValue()
											+ (parent != null ? parent.hashCode() : ""));
								outf = outfileBasename + "_" + cs.similarCompounds.size() + "_" + suffix + ".csv";
								toCSV(outf, cs.similarCompounds);
							}
							task.finish();
							if (datasetWizardPanel != null && outfileBasename != null)
							{
								final String fOutf = outf;
								SwingUtilities.invokeLater(new Runnable()
								{
									@Override
									public void run()
									{
										datasetWizardPanel.load(fOutf);
									}
								});
							}
						}
					}
				}
			});
			th.start();
		}
	}

	public static void main(String[] args)
	{
		searchDialog(null, null, null, false);
	}
}
