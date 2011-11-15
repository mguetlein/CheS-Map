package gui;

import gui.property.IntegerProperty;
import gui.property.Property;
import gui.property.PropertyPanel;
import gui.wizard.GenericWizardPanel;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EmptyBorder;

import main.Settings;
import util.ArrayUtil;
import weka.CascadeSimpleKMeans;
import alg.Algorithm;
import alg.cluster.DatasetClusterer;
import alg.cluster.NoClusterer;
import alg.cluster.WekaClusterer;
import alg.cluster.r.AbstractRClusterer;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

public class ClusterWizardPanel extends GenericWizardPanel
{
	boolean canProceed = false;
	public static DatasetClusterer CLUSTERERS[];
	private static IntegerProperty min = new IntegerProperty("minNumClusters", 2, 1, Integer.MAX_VALUE);
	private static IntegerProperty max = new IntegerProperty("maxNumClusters", 10, 1, Integer.MAX_VALUE);
	private static Property[] PROPS = new Property[] { min, max };
	static
	{
		min.setDisplayName("minimum number of clusters");
		max.setDisplayName("maximum number of clusters");
	}

	static
	{
		CLUSTERERS = ArrayUtil.concat(DatasetClusterer.class, new DatasetClusterer[] { new NoClusterer() },
				WekaClusterer.WEKA_CLUSTERER, AbstractRClusterer.R_CLUSTERER
		// ,new DatasetClusterer[] { new StructuralClustererService() }
				);
	}

	public static DatasetClusterer getDefaultClusterer()
	{
		return new WekaClusterer(new CascadeSimpleKMeans(), PROPS);
	}

	public ClusterWizardPanel(CheSMapperWizard w)
	{
		super(w);
	}

	@Override
	public String getTitle()
	{
		return Settings.text("cluster.title");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("cluster.desc");
	}

	@Override
	protected DatasetClusterer[] getAlgorithms()
	{
		return CLUSTERERS;
	}

	public DatasetClusterer getDatasetClusterer()
	{
		return (DatasetClusterer) getSelectedAlgorithm();
	}

	@Override
	protected boolean hasSimpleView()
	{
		return true;
	}

	class SimpleClusterPanel extends SimplePanel
	{
		JRadioButton buttonYes = new JRadioButton("Yes (recommended)", true);
		JRadioButton buttonNo = new JRadioButton("No");
		PropertyPanel propertyPanel = new PropertyPanel(new Property[] { min, max }, Settings.PROPS,
				Settings.PROPERTIES_FILE);

		public SimpleClusterPanel()
		{
			ButtonGroup group = new ButtonGroup();
			group.add(buttonYes);
			group.add(buttonNo);
			ActionListener a = new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					updateSimpleSelection(e.getSource() == buttonYes);
				}
			};
			buttonYes.addActionListener(a);
			buttonNo.addActionListener(a);
			DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("p,10px,p"));
			b.setLineGapSize(Sizes.dluX(4));
			b.append(new JLabel("Cluster dataset?"), 3);
			b.nextLine();
			b.append(buttonYes);
			b.setLineGapSize(Sizes.dluX(4));
			JPanel props = new JPanel(new BorderLayout(5, 5));
			props.add(new JLabel("Applies " + getDefaultClusterer().getName() + ""), BorderLayout.NORTH);
			props.add(propertyPanel);
			b.append(props);
			b.nextLine();
			b.append(buttonNo, 3);
			b.setBorder(new EmptyBorder(5, 0, 0, 0));

			setLayout(new BorderLayout());
			add(b.getPanel());

			String simpleSelected = (String) Settings.PROPS.get(getTitle() + "-simple-yes");
			if (simpleSelected != null && simpleSelected.equals("false"))
			{
				buttonNo.setSelected(true);
				propertyPanel.setEnabled(false);
			}
		}

		private void updateSimpleSelection(boolean clusterYes)
		{
			propertyPanel.setEnabled(clusterYes);
			wizard.update();
		}

		@Override
		protected Algorithm getAlgorithm()
		{
			if (buttonYes.isSelected())
				return getDefaultClusterer();
			else
				return new NoClusterer();
		}

		@Override
		protected void store()
		{
			propertyPanel.store();
			Settings.PROPS.put(getTitle() + "-simple-yes", buttonYes.isSelected() ? "true" : "false");
		}
	}

	@Override
	protected SimplePanel createSimpleView()
	{
		return new SimpleClusterPanel();
	}

	protected int defaultSelection()
	{
		return 2;
	}

}
