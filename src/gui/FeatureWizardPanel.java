package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import main.Settings;
import main.TaskProvider;
import util.ArrayUtil;
import util.DoubleImageIcon;
import util.FileUtil;
import util.ImageLoader;
import util.SwingUtil;
import util.VectorUtil;
import util.WizardComponentFactory;
import alg.FeatureComputer;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import data.DatasetFile;
import data.DefaultFeatureComputer;
import data.IntegratedProperty;
import data.cdk.CDKPropertySet;
import data.fragments.MatchEngine;
import data.fragments.StructuralFragmentProperties;
import data.fragments.StructuralFragments;
import dataInterface.FragmentPropertySet;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import dataInterface.MoleculePropertySet;
import dataInterface.MoleculePropertySetUtil;

public class FeatureWizardPanel extends WizardPanel
{
	Selector<MoleculePropertySet> selector;
	JLabel numFeaturesLabel;
	LinkButton loadFeaturesButton = new LinkButton("Load");
	MoleculePropertyPanel moleculePropertyPanel;
	JScrollPane propertyScroll;

	DatasetFile dataset = null;
	boolean selfUpdate;

	private String addFeaturesText = "Add feature";
	private String remFeaturesText = "Remove feature";

	public static final String ROOT = "Features";
	public static final String INTEGRATED_FEATURES = Settings.text("features.integrated");
	public static final String CDK_FEATURES = Settings.text("features.cdk");
	public static final String STRUCTURAL_FRAGMENTS = Settings.text("features.struct");

	JPanel propsPanelContainer;
	private JPanel propsPanel;
	private StructuralFragmentPropertiesPanel fragmentProperties;
	CheSMapperWizard wizard;

	private JButton addSmarts;

	public FeatureWizardPanel(CheSMapperWizard wizard)
	{
		this.wizard = wizard;

		createSelector();
		buildLayout();
		addListeners();

		updateFeatureProperties(null);
	}

	private void buildLayout()
	{

		JPanel labelPanel = new JPanel(new GridLayout(0, 2, 10, 10));
		JLabel label = new JLabel("Available Features:");
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		labelPanel.add(label);
		JLabel label2 = new JLabel("Selected Features:");
		label2.setFont(label2.getFont().deriveFont(Font.BOLD));
		labelPanel.add(label2);

		selector.setAddButtonText(addFeaturesText);
		selector.setRemoveButtonText(remFeaturesText);

		numFeaturesLabel = new JLabel();
		JPanel numFeaturesPanel = new JPanel(new GridLayout(0, 2, 10, 10));
		numFeaturesPanel.add(new JLabel(""));
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
		p.add(numFeaturesLabel);
		loadFeaturesButton.setForegroundFont(loadFeaturesButton.getFont().deriveFont(Font.PLAIN));
		loadFeaturesButton.setSelectedForegroundFont(loadFeaturesButton.getFont().deriveFont(Font.PLAIN));
		loadFeaturesButton.setSelectedForegroundColor(Color.BLUE);
		p.add(loadFeaturesButton);
		numFeaturesPanel.add(p);

		JPanel selectPanel = new JPanel(new BorderLayout(5, 5));
		selectPanel.add(labelPanel, BorderLayout.NORTH);
		selectPanel.add(selector);
		selectPanel.add(numFeaturesPanel, BorderLayout.SOUTH);

		JLabel label3 = new JLabel("Feature properties:");
		label3.setFont(label3.getFont().deriveFont(Font.BOLD));

		fragmentProperties = new StructuralFragmentPropertiesPanel();

		fragmentProperties.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 10, 0), fragmentProperties.getBorder()));
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("p,fill:p:grow"));
		addSmarts = new JButton("Add SMARTS file");

		builder.append(addSmarts);
		builder.nextLine();
		builder.appendParagraphGapRow();
		builder.nextLine();
		builder.append(fragmentProperties.getSummaryPanel(), 2);
		propsPanel = builder.getPanel();

		moleculePropertyPanel = new MoleculePropertyPanel(this);
		propsPanelContainer = new JPanel(new BorderLayout());
		JPanel combinedPropertyPanel = new JPanel(new BorderLayout());
		combinedPropertyPanel.add(moleculePropertyPanel, BorderLayout.NORTH);
		combinedPropertyPanel.add(propsPanelContainer, BorderLayout.CENTER);
		combinedPropertyPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		propertyScroll = WizardComponentFactory.getVerticalScrollPane(combinedPropertyPanel);

		JPanel propsPanel = new JPanel(new BorderLayout(5, 5));
		propsPanel.add(label3, BorderLayout.NORTH);
		propsPanel.add(propertyScroll);

		setLayout(new BorderLayout(10, 10));
		add(selectPanel, BorderLayout.NORTH);
		add(propsPanel);
	}

	private void createSelector()
	{
		selector = new Selector<MoleculePropertySet>(MoleculePropertySet.class, ROOT)
		{
			public Icon getIcon(MoleculePropertySet elem)
			{
				ImageIcon warningIcon = null;
				ImageIcon typeIcon = null;

				if (elem.getBinary() != null && !elem.getBinary().isFound())
					warningIcon = ImageLoader.ERROR;

				MoleculeProperty.Type type = MoleculePropertySetUtil.getType(elem);
				if (type == Type.NUMERIC)
					typeIcon = ImageLoader.NUMERIC;
				else if (type == Type.NOMINAL)
					typeIcon = ImageLoader.DISTINCT;
				else if (elem.getSize(dataset) == 1 && warningIcon == null)
					warningIcon = ImageLoader.WARNING;

				if (!elem.isComputed(dataset) && !elem.isCached(dataset) && elem.isComputationSlow())
					warningIcon = ImageLoader.HOURGLASS;

				if (warningIcon == null)
					return typeIcon;
				if (typeIcon == null)
					return warningIcon;
				return new DoubleImageIcon(typeIcon, warningIcon);
			}

			@Override
			public boolean isValid(MoleculePropertySet elem)
			{
				if (elem.getBinary() != null && !elem.getBinary().isFound())
					return false;
				if ((!elem.isSizeDynamic() || elem.isComputed(dataset)) && elem.getSize(dataset) == 0)
					return false;
				return MoleculePropertySetUtil.getType(elem) != null;
			}

			@Override
			public ImageIcon getCategoryIcon(String name)
			{
				if (STRUCTURAL_FRAGMENTS.equals(name)
						&& StructuralFragmentProperties.getMatchEngine() == MatchEngine.OpenBabel
						&& !Settings.BABEL_BINARY.isFound())
					return ImageLoader.ERROR;
				else
					return null;
			}

			@Override
			public String getString(MoleculePropertySet elem)
			{
				if (elem.isSizeDynamic() && !elem.isComputed(dataset))
					return elem + " (? features)";
				else if (elem.isSizeDynamic() || elem.getSize(dataset) > 1)
					return elem + " (" + elem.getSize(dataset) + " features)";
				else
					return elem.toString();
			}
		};
	}

	private void addListeners()
	{
		Settings.BABEL_BINARY.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (Settings.BABEL_BINARY.isFound())
					selector.repaintSelector();
			}
		});

		selector.addPropertyChangeListener(Selector.PROPERTY_SELECTION_CHANGED, new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update();
			}
		});
		selector.addPropertyChangeListener(Selector.PROPERTY_HIGHLIGHTING_CHANGED, new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (selector.getHighlightedElement() != null)
				{
					moleculePropertyPanel.setSelectedPropertySet(selector.getHighlightedElement());
					propsPanelContainer.removeAll();
					propsPanelContainer.revalidate();
					propsPanelContainer.repaint();
				}
				else
				{
					moleculePropertyPanel.setSelectedPropertySet(null);
					updateFeatureProperties(selector.getHighlightedCategory());
				}
			}
		});
		selector.addPropertyChangeListener(Selector.PROPERTY_TRY_ADDING_INVALID, new PropertyChangeListener()
		{
			@SuppressWarnings("unchecked")
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("fill:p:grow"));
				b.append("Could not add the following feature(s)");
				for (Object o : (List<?>) evt.getNewValue())
					b.append("* " + o);
				List<MoleculePropertySet> set = (List<MoleculePropertySet>) evt.getNewValue();
				if (set.get(0).getBinary() != null && !set.get(0).getBinary().isFound())
					b.append(Settings.getBinaryComponent(set.get(0).getBinary()));
				else
					b.append("The feature(s) is/are most likely not suited for clustering and embedding.\nYou have to asign the feature type manually (by clicking on 'Nominal') before adding the feature/s.");

				JOptionPane.showMessageDialog(Settings.TOP_LEVEL_COMPONENT, b.getPanel(),
						"Warning - Could not add feature", JOptionPane.WARNING_MESSAGE);
			}
		});
		selector.addPropertyChangeListener(Selector.PROPERTY_EMPTY_ADD, new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				JOptionPane.showMessageDialog(Settings.TOP_LEVEL_COMPONENT,
						"You have no feature/s selected. Please select (a group of) feature/s in the left panel before clicking '"
								+ addFeaturesText + "'.", "Warning - No feature/s selected",
						JOptionPane.WARNING_MESSAGE);
			}
		});
		selector.addPropertyChangeListener(Selector.PROPERTY_EMPTY_REMOVE, new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				JOptionPane.showMessageDialog(Settings.TOP_LEVEL_COMPONENT,
						"You have no feature/s selected. Please select (a group of) feature/s in the right panel before clicking '"
								+ remFeaturesText + "'.", "Warning - No feature/s selected",
						JOptionPane.WARNING_MESSAGE);
			}
		});

		loadFeaturesButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Thread th = new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						try
						{
							TaskProvider.registerThread("Compute features");
							TaskProvider.task().showDialog((JFrame) FeatureWizardPanel.this.getTopLevelAncestor(),
									"Computing features");
							int num = 0;
							MoleculePropertySet sets[] = selector.getSelected();
							for (MoleculePropertySet set : sets)
								if (!set.isComputed(dataset))
									num++;
							int step = 100 / num;
							int p = 0;
							for (MoleculePropertySet set : sets)
								if (!set.isComputed(dataset))
								{
									TaskProvider.task().update(p, " Compute feature: " + set);
									p += step;
									set.compute(dataset);
									if (TaskProvider.task().isCancelled())
										break;
								}
							selector.repaint();
							FeatureWizardPanel.this.update();
							if (selector.getHighlightedElement() != null) // update chart
								moleculePropertyPanel.setSelectedPropertySet(selector.getHighlightedElement());
							TaskProvider.task().getDialog().setVisible(false);
						}
						catch (Throwable e)
						{
							e.printStackTrace();
							TaskProvider.task().error(e.getMessage(), e);
							if (TaskProvider.task().getDialog() != null)
								SwingUtil.waitWhileVisible(TaskProvider.task().getDialog());
						}
						finally
						{
							TaskProvider.clear();
						}
					}
				});
				th.start();
			}
		});

		addSmarts.addActionListener(new ActionListener()
		{
			JFileChooser fc;

			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (fc == null)
					fc = new JFileChooser();
				int res = fc.showOpenDialog(Settings.TOP_LEVEL_COMPONENT);
				if (res == JFileChooser.APPROVE_OPTION)
				{
					File f = fc.getSelectedFile();
					String name = FileUtil.getFilename(f.getAbsolutePath());
					String dest = Settings.getFragmentFileDestination(name);
					if (new File(dest).exists())
					{
						int res2 = JOptionPane.showConfirmDialog(Settings.TOP_LEVEL_COMPONENT, "Smarts file '" + name
								+ "' already exists. Replace?", "File already exists", JOptionPane.YES_OPTION);
						if (res2 != JOptionPane.YES_OPTION)
							return;
					}
					FileUtil.copy(f, new File(dest));
					StructuralFragments.instance.reset(name);
					updateIntegratedFeatures(dataset, true);
				}
			}
		});

		moleculePropertyPanel.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(MoleculePropertyPanel.PROPERTY_TYPE_CHANGED)
						|| evt.getPropertyName().equals(MoleculePropertyPanel.PROPERTY_COMPUTED))
				{
					selector.repaintSelector();
					update();
				}
			}
		});

		StructuralFragments.instance.toString(); // load instances first (listener order is important)
		StructuralFragmentProperties.addPropertyChangeListenerToProperties(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				selector.repaintSelector();
				update();
				// update chart
				if (selector.getHighlightedElement() != null)
					moleculePropertyPanel.setSelectedPropertySet(selector.getHighlightedElement());
			}
		});
	}

	protected void updateFeatureProperties(String highlightedCategory)
	{
		JPanel props = null;

		String info;
		if (highlightedCategory == null || highlightedCategory.equals(ROOT))
			info = Settings.text("features.desc.long", addFeaturesText);
		else if (highlightedCategory.equals(INTEGRATED_FEATURES))
			info = Settings.text("features.integrated.desc");
		else if (highlightedCategory.equals(STRUCTURAL_FRAGMENTS))
		{
			info = Settings.text("features.struct.desc", Settings.STRUCTURAL_FRAGMENT_DIR + File.separator);
			props = propsPanel;
		}
		else if (highlightedCategory.equals(CDK_FEATURES))
			info = Settings.text("features.cdk.desc", Settings.CDK_STRING);
		else
		{
			//cdk sub categories
			info = Settings.text("features.cdk.desc", Settings.CDK_STRING);
		}
		moleculePropertyPanel.showInfoText(info);
		if (props == null)
			propsPanelContainer.removeAll();
		else
			propsPanelContainer.add(props, BorderLayout.WEST);
		propsPanelContainer.revalidate();
		propsPanelContainer.repaint();
	}

	int selectedPropertyIndex = 0;
	List<MoleculeProperty> selectedProperties = new ArrayList<MoleculeProperty>();
	FeatureInfo featureInfo;
	Messages msg;

	public void update()
	{
		boolean notComputed = false;
		boolean slowFeaturesSelected = false;
		msg = null;
		featureInfo = new FeatureInfo();
		for (MoleculePropertySet set : selector.getSelected())
		{
			featureInfo.featuresSelected = true;
			if (set.isSizeDynamic() && !set.isComputed(dataset))
			{
				featureInfo.numFeaturesUnknown = true;
				if (set.isSizeDynamicHigh(dataset))
					featureInfo.numFeaturesUnknownHigh = true;
			}
			else
			{
				int num = set.getSize(dataset);
				featureInfo.numFeatures += num;
			}
			if (set instanceof FragmentPropertySet)
				featureInfo.smartsFeaturesSelected = true;
			if (!set.isComputed(dataset))
				notComputed = true;
			if (!set.isComputed(dataset) && !set.isCached(dataset) && set.isComputationSlow())
				slowFeaturesSelected = true;
			if (set.getType() == Type.NUMERIC)
				featureInfo.numericFeaturesSelected = true;
			if (set.getType() == Type.NOMINAL)
				featureInfo.nominalFeaturesSelected = true;
		}
		numFeaturesLabel.setText("Number of selected features: " + featureInfo.numFeatures
				+ (featureInfo.numFeaturesUnknown ? " + ?" : ""));

		if (slowFeaturesSelected)
			msg = Messages.slowMessage(Settings.text("features.slow"));
		loadFeaturesButton.setVisible(notComputed);
		if (!selfUpdate)
			wizard.update();
	}

	public FeatureInfo getFeatureInfo()
	{
		return featureInfo;
	}

	public static class FeatureInfo
	{
		public int numFeatures = 0;
		public boolean numFeaturesUnknown = false;
		public boolean numFeaturesUnknownHigh = false;
		public boolean featuresSelected = false;
		public boolean numericFeaturesSelected = false;
		public boolean nominalFeaturesSelected = false;
		public boolean smartsFeaturesSelected = false;

		public boolean isNumFeaturesHigh()
		{
			if (numFeatures >= 1000)
				return true;
			if (numFeaturesUnknown)
				return numFeaturesUnknownHigh;
			return false;
		}

		public String getNumFeaturesWarning()
		{
			if (!isNumFeaturesHigh())
				throw new IllegalStateException();
			String msg = "The selected algorithm could run a long time because the number of features is ";
			if (numFeatures >= 1000)
			{
				msg += "high (" + (numFeaturesUnknown ? ">=" : "") + numFeatures + ").";
				//				if (smartsFeaturesSelected)
				//					msg += " You could try to increase minimum-frequency ";
				//				else
				//					msg += " You could select less features ";
				//				msg += "in the feature wizard step.";
			}
			else
				msg += "probably high.";
			//						+ " Try precomputing the features in the feature wizard step" + " by pressing '"
			//						+ MoleculePropertyPanel.LOAD_FEATURE_VALUES + "').";
			return msg;
		}
	}

	public void updateIntegratedFeatures(DatasetFile dataset)
	{
		updateIntegratedFeatures(dataset, false);
	}

	private void updateIntegratedFeatures(DatasetFile dataset, boolean forceUpdate)
	{
		if (dataset.equals(this.dataset) && !forceUpdate)
			return;

		this.dataset = dataset;
		moleculePropertyPanel.setDataset(dataset);

		selfUpdate = true;

		MoleculePropertySet[] selected = selector.getSelected();
		selector.clearElements();

		IntegratedProperty[] integrated = dataset.getIntegratedProperties(true);
		selector.addElementList(INTEGRATED_FEATURES, integrated);
		for (CDKPropertySet p : CDKPropertySet.NUMERIC_DESCRIPTORS)
			for (String clazz : p.getDictionaryClass())
				selector.addElements(new String[] { CDK_FEATURES, clazz }, p);
		selector.addElements(STRUCTURAL_FRAGMENTS);
		selector.addElementList(STRUCTURAL_FRAGMENTS, StructuralFragments.instance.getSets());

		String integratedFeatures = (String) Settings.PROPS.get("features-integrated");
		Vector<String> selection = VectorUtil.fromCSVString(integratedFeatures);
		for (String string : selection)
		{
			int index = string.lastIndexOf('#');
			if (index == -1)
				throw new Error("no type in serialized molecule prop");
			Type t = Type.valueOf(string.substring(index + 1));
			String feat = string.substring(0, index);

			IntegratedProperty p = IntegratedProperty.fromString(feat, t, dataset);
			selector.setSelected(p);
		}

		String cdkFeatures = (String) Settings.PROPS.get("features-cdk");
		selection = VectorUtil.fromCSVString(cdkFeatures);
		for (String string : selection)
		{
			CDKPropertySet d = CDKPropertySet.fromString(string);
			selector.setSelected(d);
		}

		String fragmentFeatures = (String) Settings.PROPS.get("features-fragments");
		selection = VectorUtil.fromCSVString(fragmentFeatures);
		for (String string : selection)
		{
			FragmentPropertySet d = StructuralFragments.instance.findFromString(string);
			if (d != null)
				selector.setSelected(d);
		}

		selector.setSelected(selected);

		update();
		selfUpdate = false;
	}

	@Override
	public void proceed()
	{
		MoleculePropertySet[] integratedProps = selector.getSelected(INTEGRATED_FEATURES);
		String[] serilizedProps = new String[integratedProps.length];
		for (int i = 0; i < serilizedProps.length; i++)
			serilizedProps[i] = integratedProps[i] + "#" + integratedProps[i].getType();
		Settings.PROPS.put("features-integrated", ArrayUtil.toCSVString(serilizedProps, true));

		Settings.PROPS.put("features-cdk", ArrayUtil.toCSVString(selector.getSelected(CDK_FEATURES), true));
		Settings.PROPS.put("features-fragments",
				ArrayUtil.toCSVString(selector.getSelected(STRUCTURAL_FRAGMENTS), true));
		Settings.storeProps();

		if (fragmentProperties != null)
			fragmentProperties.store();
	}

	@Override
	public Messages canProceed()
	{
		return msg;
	}

	public FeatureComputer getFeatureComputer()
	{
		return new DefaultFeatureComputer(selector.getSelected());
	}

	@Override
	public String getTitle()
	{
		return Settings.text("features.title");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("features.desc");
	}

	public StructuralFragmentPropertiesPanel getFragmentPropPanel()
	{
		return fragmentProperties;
	}

}
