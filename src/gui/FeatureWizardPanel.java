package gui;

import gui.property.Property;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import main.BinHandler;
import main.PropHandler;
import main.ScreenSetup;
import main.Settings;
import util.ArrayUtil;
import util.DoubleImageIcon;
import util.FileUtil;
import util.ImageLoader;
import util.MessageUtil;
import util.StringUtil;
import util.WizardComponentFactory;
import workflow.FeatureMappingWorkflowProvider;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import data.DatasetFile;
import data.FeatureLoader;
import data.FeatureService;
import data.IntegratedProperty;
import data.cdk.CDKPropertySet;
import data.fragments.MatchEngine;
import data.fragments.StructuralFragmentProperties;
import data.fragments.StructuralFragments;
import data.obdesc.OBDescriptorProperty;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;
import dataInterface.CompoundPropertySet;
import dataInterface.CompoundPropertySetUtil;
import dataInterface.FragmentPropertySet;

public class FeatureWizardPanel extends WizardPanel implements FeatureMappingWorkflowProvider
{
	Selector<CompoundPropertySet> selector;
	JLabel numFeaturesLabel;
	LinkButton loadFeaturesButton = new LinkButton("Load");
	CompoundPropertyPanel compoundPropertyPanel;
	JScrollPane propertyScroll;

	DatasetFile dataset = null;
	boolean selfUpdate;

	private String addFeaturesText = "Add feature";
	private String remFeaturesText = "Remove feature";

	public static final String ROOT = "Features";
	public static final String INTEGRATED_FEATURES = Settings.text("features.integrated");
	public static final String CDK_FEATURES = Settings.text("features.cdk");
	public static final String OB_FEATURES = Settings.text("features.ob");
	public static final String STRUCTURAL_FRAGMENTS = Settings.text("features.struct");

	JPanel structuralFragmentPropsContainer;
	private JPanel propsPanel;
	private StructuralFragmentPropertiesPanel fragmentProperties;
	CheSMapperWizard wizard;

	private JButton addSmarts;

	public FeatureWizardPanel()
	{
		this(null);
	}

	public FeatureWizardPanel(CheSMapperWizard wizard)
	{
		this.wizard = wizard;
		if (wizard == null)
			return;

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
		builder.append(fragmentProperties.getSummaryPanel(wizard), 2);
		propsPanel = builder.getPanel();

		compoundPropertyPanel = new CompoundPropertyPanel(this, wizard);
		structuralFragmentPropsContainer = new JPanel(new BorderLayout());
		JPanel combinedPropertyPanel = new JPanel(new BorderLayout());
		combinedPropertyPanel.add(compoundPropertyPanel, BorderLayout.NORTH);
		combinedPropertyPanel.add(structuralFragmentPropsContainer, BorderLayout.CENTER);
		combinedPropertyPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		propertyScroll = WizardComponentFactory.getVerticalScrollPane(combinedPropertyPanel);

		//		SwingUtil.setDebugBorder(structuralFragmentPropsContainer, Color.GREEN);

		JPanel propsPanel = new JPanel(new BorderLayout(5, 5));
		propsPanel.add(label3, BorderLayout.NORTH);
		propsPanel.add(propertyScroll);

		setLayout(new BorderLayout(10, 10));
		add(selectPanel, BorderLayout.NORTH);
		add(propsPanel);
	}

	private void createSelector()
	{
		selector = new Selector<CompoundPropertySet>(CompoundPropertySet.class, ROOT,
				(ScreenSetup.SETUP.isWizardSpaceSmall() ? 6 : 12))
		{
			public Icon getIcon(CompoundPropertySet elem)
			{
				ImageIcon warningIcon = null;
				ImageIcon typeIcon = null;

				if (elem.getBinary() != null && !elem.getBinary().isFound())
					warningIcon = ImageLoader.ERROR;

				CompoundProperty.Type type = CompoundPropertySetUtil.getType(elem);
				if (type == Type.NUMERIC)
					typeIcon = ImageLoader.NUMERIC;
				else if (type == Type.NOMINAL)
					typeIcon = ImageLoader.DISTINCT;
				else if (elem.getSize(dataset) == 1 && warningIcon == null)
					warningIcon = ImageLoader.WARNING;

				if (!elem.isComputed(dataset) && !(Settings.CACHING_ENABLED && elem.isCached(dataset))
						&& elem.isComputationSlow())
					warningIcon = ImageLoader.HOURGLASS;

				if (warningIcon == null)
					return typeIcon;
				if (typeIcon == null)
					return warningIcon;
				return new DoubleImageIcon(typeIcon, warningIcon);
			}

			@Override
			public boolean isValid(CompoundPropertySet elem)
			{
				if (elem.getBinary() != null && !elem.getBinary().isFound())
					return false;
				if ((!elem.isSizeDynamic() || elem.isComputed(dataset)) && elem.getSize(dataset) == 0)
					return false;
				return CompoundPropertySetUtil.getType(elem) != null;
			}

			@Override
			public ImageIcon getCategoryIcon(String name)
			{
				if (!BinHandler.BABEL_BINARY.isFound()
						&& ((STRUCTURAL_FRAGMENTS.equals(name) && StructuralFragmentProperties.getMatchEngine() == MatchEngine.OpenBabel) || OB_FEATURES
								.equals(name)))
					return ImageLoader.ERROR;
				else
					return null;
			}

			@Override
			public String getString(CompoundPropertySet elem)
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
		BinHandler.BABEL_BINARY.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (BinHandler.BABEL_BINARY.isFound())
				{
					selector.addElementList(OB_FEATURES, OBDescriptorProperty.getDescriptors(true));
					update();
				}
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
					compoundPropertyPanel.setSelectedPropertySet(selector.getHighlightedElement());
					structuralFragmentPropsContainer.removeAll();
					structuralFragmentPropsContainer.revalidate();
					structuralFragmentPropsContainer.repaint();
				}
				else
				{
					compoundPropertyPanel.setSelectedPropertySet(null);
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
				List<CompoundPropertySet> set = (List<CompoundPropertySet>) evt.getNewValue();
				if (set.get(0).getBinary() != null && !set.get(0).getBinary().isFound())
					b.append(BinHandler.getBinaryComponent(set.get(0).getBinary(), wizard));
				else
					b.append("The feature(s) is/are most likely not suited for clustering and embedding.\nYou have to asign the feature type manually (by clicking on 'Nominal') before adding the feature/s.");

				JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, b.getPanel(),
						"Warning - Could not add feature", JOptionPane.WARNING_MESSAGE);
			}
		});
		selector.addPropertyChangeListener(Selector.PROPERTY_EMPTY_ADD, new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME,
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
				JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME,
						"You have no feature/s selected. Please select (a group of) feature/s in the right panel before clicking '"
								+ remFeaturesText + "'.", "Warning - No feature/s selected",
						JOptionPane.WARNING_MESSAGE);
			}
		});
		selector.addKeyListener(new KeyAdapter()
		{
			public void keyTyped(KeyEvent e)
			{
				if (e.getKeyChar() == '#')
				{
					CompoundPropertySet p = (CompoundPropertySet) selector.getHighlightedElement();
					Settings.LOGGER.info("toggle type for " + p);
					compoundPropertyPanel.toggleType();
				}
			}
		});

		FeatureLoader.instance.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update();
			}
		});

		loadFeaturesButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				FeatureLoader.instance.loadFeatures(selector.getSelected(), dataset,
						(Window) FeatureWizardPanel.this.getTopLevelAncestor());
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
				int res = fc.showOpenDialog(Settings.TOP_LEVEL_FRAME);
				if (res == JFileChooser.APPROVE_OPTION)
				{
					File f = fc.getSelectedFile();
					String name = FileUtil.getFilename(f.getAbsolutePath());
					String dest = Settings.getFragmentFileDestination(name);
					if (new File(dest).exists())
					{
						int res2 = JOptionPane.showConfirmDialog(Settings.TOP_LEVEL_FRAME, "Smarts file '" + name
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

		compoundPropertyPanel.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(CompoundPropertyPanel.PROPERTY_TYPE_CHANGED)
						|| evt.getPropertyName().equals(CompoundPropertyPanel.PROPERTY_CACHED_FEATURE_LOADED))
					update();
			}
		});

		StructuralFragments.instance.toString(); // load instances first (listener order is important)
		StructuralFragmentProperties.addPropertyChangeListenerToProperties(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update();
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
		else if (highlightedCategory.equals(OB_FEATURES))
		{
			info = Settings.text("features.ob.desc", Settings.OPENBABEL_STRING);
			JComponent pp = BinHandler.getBinaryComponent(BinHandler.BABEL_BINARY, wizard);
			JPanel p = new JPanel();
			p.add(pp);
			props = p;
		}
		else
		{
			//cdk sub categories
			info = Settings.text("features.cdk.desc", Settings.CDK_STRING);
		}
		compoundPropertyPanel.showInfoText(info);
		if (props == null)
			structuralFragmentPropsContainer.removeAll();
		else
			structuralFragmentPropsContainer.add(props, BorderLayout.WEST);
		structuralFragmentPropsContainer.revalidate();
		structuralFragmentPropsContainer.repaint();
	}

	int selectedPropertyIndex = 0;
	List<CompoundProperty> selectedProperties = new ArrayList<CompoundProperty>();
	FeatureInfo featureInfo;
	Messages msg;

	public void update()
	{
		if (wizard == null)
			return;

		selector.repaintSelector();

		boolean notComputed = false;
		boolean slowFeaturesSelected = false;
		msg = null;
		featureInfo = new FeatureInfo();
		for (CompoundPropertySet set : selector.getSelected())
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
			if (!set.isComputed(dataset) && !(Settings.CACHING_ENABLED && set.isCached(dataset))
					&& set.isComputationSlow())
				slowFeaturesSelected = true;
			if (set.getType() == Type.NUMERIC)
				featureInfo.numericFeaturesSelected = true;
			if (set.getType() == Type.NOMINAL)
				featureInfo.nominalFeaturesSelected = true;
		}
		numFeaturesLabel.setText("Number of selected features: " + featureInfo.numFeatures
				+ (featureInfo.numFeaturesUnknown ? " + ?" : ""));

		if (slowFeaturesSelected)
			msg = MessageUtil.slowMessages(Settings.text("features.slow"));
		loadFeaturesButton.setVisible(notComputed);
		if (!selfUpdate)
			wizard.update();

		if (selector.getHighlightedElement() != null)
			compoundPropertyPanel.setSelectedPropertySet(selector.getHighlightedElement());
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
			//						+ CompoundPropertyPanel.LOAD_FEATURE_VALUES + "').";
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
		if (wizard == null)
			return;

		compoundPropertyPanel.setDataset(dataset);

		selfUpdate = true;

		CompoundPropertySet[] selected = selector.getSelected();
		selector.clearElements();

		IntegratedProperty[] integrated = dataset.getIntegratedProperties();
		selector.addElementList(INTEGRATED_FEATURES, integrated);
		for (CDKPropertySet p : CDKPropertySet.NUMERIC_DESCRIPTORS)
			for (String clazz : p.getDictionaryClass())
				selector.addElements(new String[] { CDK_FEATURES, clazz }, p);

		selector.addElementList(OB_FEATURES, OBDescriptorProperty.getDescriptors(false));
		selector.addElements(STRUCTURAL_FRAGMENTS);
		selector.addElementList(STRUCTURAL_FRAGMENTS, StructuralFragments.instance.getSets());

		for (CompoundPropertySet m : getFeaturesFromMappingWorkflow(PropHandler.getProperties(), false))
			selector.setSelected(m);

		selector.setSelected(selected);

		update();
		selfUpdate = false;
	}

	@Override
	public CompoundPropertySet[] getFeaturesFromMappingWorkflow(Properties props, boolean storeToSettings)
	{
		for (Property p : StructuralFragmentProperties.getProperties())
			p.loadOrResetToDefault(props);
		if (storeToSettings)
		{
			for (String propKey : allPropKeys)
				if (props.containsKey(propKey))
					PropHandler.put(propKey, (String) props.get(propKey));
				else
					PropHandler.remove(propKey);
			for (Property p : StructuralFragmentProperties.getProperties())
				p.put(PropHandler.getProperties());
		}

		List<CompoundPropertySet> features = new ArrayList<CompoundPropertySet>();

		String integratedFeatures = (String) props.get(propKeyIntegrated);
		List<String> selection = StringUtil.split(integratedFeatures);
		for (String string : selection)
		{
			int index = string.lastIndexOf('#');
			if (index == -1)
				throw new Error("no type in serialized compound prop");
			Type t = Type.valueOf(string.substring(index + 1));
			String feat = string.substring(0, index);

			IntegratedProperty p = IntegratedProperty.fromString(feat, t, dataset);
			features.add(p);
		}

		String cdkFeatures = (String) props.get(propKeyCDK);
		selection = StringUtil.split(cdkFeatures);
		for (String string : selection)
		{
			CDKPropertySet d = CDKPropertySet.fromString(string);
			features.add(d);
		}

		String obFeatures = (String) props.get(propKeyOB);
		selection = StringUtil.split(obFeatures);
		for (String string : selection)
		{
			if (string == null)
				continue;
			int index = string.lastIndexOf('#');
			if (index == -1)
				throw new Error("no type in serialized compound prop");
			Type t = Type.valueOf(string.substring(index + 1));
			String feat = string.substring(0, index);
			OBDescriptorProperty p = OBDescriptorProperty.fromString(feat, t);
			features.add(p);
		}

		String fragmentFeatures = (String) props.get(propKeyFragments);
		selection = StringUtil.split(fragmentFeatures);
		for (String string : selection)
		{
			FragmentPropertySet d = StructuralFragments.instance.findFromString(string);
			if (d != null)
				features.add(d);
		}

		return ArrayUtil.toArray(CompoundPropertySet.class, features);
	}

	private String propKeyIntegrated = "features-integrated";
	private String propKeyCDK = "features-cdk";
	private String propKeyOB = "features-ob";
	private String propKeyFragments = "features-fragments";
	private String[] allPropKeys = { propKeyIntegrated, propKeyCDK, propKeyOB, propKeyFragments };

	@Override
	public void proceed()
	{
		HashMap<String, CompoundPropertySet[]> selected = new HashMap<String, CompoundPropertySet[]>();
		selected.put(INTEGRATED_FEATURES, selector.getSelected(INTEGRATED_FEATURES));
		selected.put(CDK_FEATURES, selector.getSelected(CDK_FEATURES));
		selected.put(OB_FEATURES, selector.getSelected(OB_FEATURES));
		selected.put(STRUCTURAL_FRAGMENTS, selector.getSelected(STRUCTURAL_FRAGMENTS));
		putToProps(selected, PropHandler.getProperties());

		PropHandler.storeProperties();
		if (fragmentProperties != null)
			fragmentProperties.store();
	}

	private void putToProps(HashMap<String, CompoundPropertySet[]> features, Properties props)
	{
		if (features.containsKey(INTEGRATED_FEATURES) && features.get(INTEGRATED_FEATURES).length > 0)
		{
			IntegratedProperty[] integratedProps = ArrayUtil.cast(IntegratedProperty.class,
					features.get(INTEGRATED_FEATURES));
			String[] serilizedProps = new String[integratedProps.length];
			for (int i = 0; i < serilizedProps.length; i++)
				serilizedProps[i] = integratedProps[i] + "#" + integratedProps[i].getType();
			props.put(propKeyIntegrated, ArrayUtil.toCSVString(serilizedProps, true));
		}
		else
			props.remove(propKeyIntegrated);

		if (CDKPropertySet.NUMERIC_DESCRIPTORS.length > 0)
		{
			if (features.containsKey(CDK_FEATURES) && features.get(CDK_FEATURES).length > 0)
				props.put(propKeyCDK, ArrayUtil.toCSVString(features.get(CDK_FEATURES), true));
			else
				props.remove(propKeyCDK);
		}

		if (features.containsKey(OB_FEATURES) && features.get(OB_FEATURES).length > 0)
		{
			OBDescriptorProperty[] obFeats = ArrayUtil.cast(OBDescriptorProperty.class, features.get(OB_FEATURES));
			String[] serilizedOBFeats = new String[obFeats.length];
			for (int i = 0; i < serilizedOBFeats.length; i++)
				serilizedOBFeats[i] = obFeats[i] + "#" + obFeats[i].getType();
			props.put(propKeyOB, ArrayUtil.toCSVString(serilizedOBFeats, true));
		}
		else
			props.remove(propKeyOB);

		if (features.containsKey(STRUCTURAL_FRAGMENTS) && features.get(STRUCTURAL_FRAGMENTS).length > 0)
			props.put(propKeyFragments, ArrayUtil.toCSVString(features.get(STRUCTURAL_FRAGMENTS), true));
		else
			props.remove(propKeyFragments);

		for (Property p : StructuralFragmentProperties.getProperties())
			p.put(props);
	}

	@Override
	public Messages canProceed()
	{
		return msg;
	}

	public CompoundPropertySet[] getSelectedFeatures()
	{
		return selector.getSelected();
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

	@Override
	public void exportFeaturesToMappingWorkflow(HashMap<String, CompoundPropertySet[]> features, Properties props)
	{
		for (CompoundPropertySet[] feats : features.values())
			for (CompoundPropertySet feature : feats)
			{
				if (feature instanceof IntegratedProperty || feature instanceof OBDescriptorProperty)
				{
					CompoundProperty feat = (CompoundProperty) feature;
					if (feature instanceof IntegratedProperty)
					{
						if (FeatureService.guessNominalFeatureType(feat.numDistinctValues(dataset),
								dataset.numCompounds(), feat.isTypeAllowed(Type.NUMERIC)))
							feat.setType(Type.NOMINAL);
						else
							feat.setType(Type.NUMERIC);
					}
					else if (feature instanceof OBDescriptorProperty)
					{
						if (feat.isTypeAllowed(Type.NUMERIC))
							feat.setType(Type.NUMERIC);
					}
					if (feature.getType() == null)
						throw new IllegalArgumentException("probably not suited: " + feat);
				}
			}
		putToProps(features, props);
	}

	//	public void exportFeaturesToMappingWorkflow(String[] featureNames, boolean selectAllInternalFeatures,
	//			Properties props)
	//	{
	//		IntegratedProperty[] availableIntegratedProps = dataset.getIntegratedProperties(false);
	//		if (featureNames != null && featureNames.length > 0 && selectAllInternalFeatures)
	//			throw new IllegalArgumentException("either specify features manually, or select all internal features");
	//		if (selectAllInternalFeatures)
	//		{
	//			List<String> features = new ArrayList<String>();
	//			for (int i = 0; i < availableIntegratedProps.length; i++)
	//				if (availableIntegratedProps[i].isTypeAllowed(Type.NUMERIC)
	//						|| availableIntegratedProps[i].getType() == Type.NOMINAL)
	//					features.add(availableIntegratedProps[i].getName());
	//			featureNames = ArrayUtil.toArray(String.class, features);
	//		}
	//		IntegratedProperty[] selectedIntegratedProps = new IntegratedProperty[featureNames.length];
	//		int index = 0;
	//		for (String featureName : featureNames)
	//		{
	//			IntegratedProperty match = null;
	//			for (IntegratedProperty availableProp : availableIntegratedProps)
	//			{
	//				if (availableProp.getName().equals(featureName))
	//				{
	//					match = availableProp;
	//					break;
	//				}
	//			}
	//			if (match == null)
	//				throw new IllegalArgumentException("could not find property: " + featureName + " in "
	//						+ ArrayUtil.toString(availableIntegratedProps));
	//			if (match.isTypeAllowed(Type.NUMERIC))
	//				match.setType(Type.NUMERIC);
	//			else if (match.getType() != Type.NOMINAL)
	//				throw new IllegalArgumentException("probably not suited: " + featureName);
	//			selectedIntegratedProps[index++] = match;
	//		}
	//		putIntegratedToProps(selectedIntegratedProps, props);
	//	}

	@Override
	public void exportSettingsToMappingWorkflow(Properties props)
	{
		for (String propKey : allPropKeys)
			if (PropHandler.containsKey(propKey))
				props.put(propKey, PropHandler.get(propKey));
		for (Property p : StructuralFragmentProperties.getProperties())
			p.put(props);
	}
}
