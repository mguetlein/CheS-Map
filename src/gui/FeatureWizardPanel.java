package gui;

import gui.binloc.Binary;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import main.Settings;
import util.ArrayUtil;
import util.ImageLoader;
import util.VectorUtil;
import alg.FeatureComputer;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import data.CDKProperty;
import data.CDKProperty.CDKDescriptor;
import data.DatasetFile;
import data.DefaultFeatureComputer;
import data.IntegratedProperty;
import data.StructuralFragments;
import data.StructuralFragments.MatchEngine;
import dataInterface.FragmentPropertySet;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import dataInterface.MoleculePropertySet;
import dataInterface.MoleculePropertySetUtil;

public class FeatureWizardPanel extends WizardPanel
{
	Selector<MoleculePropertySet> selector;
	JLabel numFeaturesLabel;
	JPanel binaryPanelContainer;
	MoleculePropertyPanel moleculePropertyPanel;

	DatasetFile dataset = null;
	int numSelected;
	Type selectedFeatureType;
	boolean selfUpdate;

	private String addFeaturesText = "Add feature";
	private String remFeaturesText = "Remove feature";

	public static final String ROOT = "Features";
	public static final String INTEGRATED_FEATURES = "Included in Dataset";
	public static final String CDK_FEATURES = "CDK Descriptors";
	public static final String STRUCTURAL_FRAGMENTS = "Structural Fragments";

	JPanel propsPanelContainer;
	private StructuralFragmentPropertiesPanel fragmentProperties;

	public FeatureWizardPanel(final CheSMapperWizard wizard)
	{
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("min:grow"));
		numFeaturesLabel = new JLabel();

		JPanel lPanel = new JPanel(new GridLayout(0, 2, 10, 10));
		lPanel.add(new JLabel("Available Features:"));
		lPanel.add(new JLabel("Selected Features:"));
		builder.append(lPanel);
		builder.nextLine();

		Settings.BABEL_BINARY.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (Settings.BABEL_BINARY.isFound())
					selector.repaintSelector();
			}
		});

		selector = new Selector<MoleculePropertySet>(MoleculePropertySet.class, ROOT)
		{
			public ImageIcon getIcon(MoleculePropertySet elem)
			{
				if (elem.getBinary() != null && !elem.getBinary().isFound())
					return ImageLoader.ERROR;

				MoleculeProperty.Type type = MoleculePropertySetUtil.getType(elem);
				if (type == Type.NUMERIC)
					return ImageLoader.NUMERIC;
				else if (type == Type.NOMINAL)
					return ImageLoader.DISTINCT;
				else
				{
					if (elem.getSize(dataset) == 1)
						return ImageLoader.WARNING;
					else
						return null;
				}
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
				if (STRUCTURAL_FRAGMENTS.equals(name) && fragmentProperties.getMatchEngine() == MatchEngine.OpenBabel
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

		selector.setAddButtonText(addFeaturesText);
		selector.setRemoveButtonText(remFeaturesText);
		selector.addPropertyChangeListener(Selector.PROPERTY_SELECTION_CHANGED, new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update();
				if (!selfUpdate)
					wizard.update();
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
					binaryPanelContainer.removeAll();
					binaryPanelContainer.revalidate();
					binaryPanelContainer.repaint();

					propsPanelContainer.removeAll();
					propsPanelContainer.revalidate();
					propsPanelContainer.repaint();
				}
				else
				{
					moleculePropertyPanel.setSelectedPropertySet(null);
					updateFeatureInfo(selector.getHighlightedCategory());
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

		builder.append(selector);
		builder.nextLine();

		JPanel lPanel2 = new JPanel(new GridLayout(0, 2, 10, 10));
		lPanel2.add(new JLabel(""));
		lPanel2.add(numFeaturesLabel);
		builder.append(lPanel2);
		builder.nextLine();

		//		builder.appendParagraphGapRow();
		//		builder.nextLine();

		builder.appendSeparator("Feature properties");

		fragmentProperties = new StructuralFragmentPropertiesPanel();
		moleculePropertyPanel = new MoleculePropertyPanel(this);
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

		builder.append(moleculePropertyPanel);
		moleculePropertyPanel.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 10, 0), moleculePropertyPanel
				.getBorder()));
		builder.nextLine();

		propsPanelContainer = new JPanel(new BorderLayout());
		builder.append(propsPanelContainer);
		builder.nextLine();

		StructuralFragments.instance.setMinFrequency(fragmentProperties.getMinFrequency());
		StructuralFragments.instance.setSkipOmniFragments(fragmentProperties.isSkipOmniFragments());
		StructuralFragments.instance.setMatchEngine(fragmentProperties.getMatchEngine());
		fragmentProperties.addPropertyChangeListenerToProperties(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				StructuralFragments.instance.setMinFrequency(fragmentProperties.getMinFrequency());
				StructuralFragments.instance.setSkipOmniFragments(fragmentProperties.isSkipOmniFragments());
				StructuralFragments.instance.setMatchEngine(fragmentProperties.getMatchEngine());
				selector.repaintSelector();
				update();
				// to show/hide open babel binary link:
				updateFeatureInfo(selector.getHighlightedCategory());
				// update chart
				moleculePropertyPanel.setSelectedPropertySet(selector.getHighlightedElement());
			}
		});
		fragmentProperties.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 10, 0), fragmentProperties.getBorder()));

		binaryPanelContainer = new JPanel(new BorderLayout());
		builder.append(binaryPanelContainer);
		builder.nextLine();

		setLayout(new BorderLayout());
		add(builder.getPanel());

		updateFeatureInfo(null);
	}

	protected void updateFeatureInfo(String highlightedCategory)
	{
		Binary bin = null;
		JPanel props = null;

		String info = "The available features are shown in the left panel. Select (a group of) feature/s and click '"
				+ addFeaturesText
				+ "'. The selected features - shown in the right panel - will be used for clustering and/or embedding.\n\n"
				+ "The clustering/embedding result relies on the selected features. For example, select structural features (e.g. OpenBabel Fingerprint FP2) to cluster structural similar compounds together and to place structural similar compounds close together in 3D space.\n\n"
				+ "Consider carefully how many/which feature/s to chose. "
				+ "Select only a handfull of features to increase the influence of each single feature on the clustering and embedding. "
				+ "Selecting a bunch of features will effect the clustering and embedding result to represent 'overall' similarity.";
		if (highlightedCategory != null && !highlightedCategory.equals(ROOT))
		{
			if (highlightedCategory.equals(INTEGRATED_FEATURES))
				info = "Features that are already included in the provided dataset.\n"
						+ "Not all features may be suitable for clustering and/or embedding (like for example SMILES strings, or info text).";
			else if (highlightedCategory.equals(CDK_FEATURES))
				info = "Uses "
						+ Settings.CDK_STRING
						+ ".\n\n"
						+ "This integrated library can compute a range of numeric chemical descriptors (like LogP or Weight).";
			else if (highlightedCategory.equals(STRUCTURAL_FRAGMENTS))
			{
				info = "Structural fragments are molecular subgraph that are defined via smarts strings."
						+ "\nEach fragment is used as a binary nominal feature (1 => subgraph occurs, 0 => subgraph does not occur)."
						+ "\nCopy a smarts.csv into the following folder to integrate any structural fragments: "
						+ Settings.STRUCTURAL_FRAGMENT_DIR
						+ File.separator
						+ "\nEach line in the csv-file should have 2 values, name of the smarts string and smarts string. "
						+ "Comments (starting with '#') will be printed as description. Example csv file:"
						+ "\n\"Benzene\",\"c1ccccc1\"" + "\n\"Carbonyl with Carbon\",\"[CX3](=[OX1])C\"";
				props = fragmentProperties;
				if (fragmentProperties.getMatchEngine() == MatchEngine.OpenBabel)
					bin = Settings.BABEL_BINARY;
			}
		}
		moleculePropertyPanel.showInfoText(info);

		if (bin == null)
			binaryPanelContainer.removeAll();
		else
			binaryPanelContainer.add(Settings.getBinaryComponent(bin), BorderLayout.WEST);
		binaryPanelContainer.revalidate();
		binaryPanelContainer.repaint();

		if (props == null)
			propsPanelContainer.removeAll();
		else
			propsPanelContainer.add(props, BorderLayout.WEST);
		propsPanelContainer.revalidate();
		propsPanelContainer.repaint();
	}

	int selectedPropertyIndex = 0;
	List<MoleculeProperty> selectedProperties = new ArrayList<MoleculeProperty>();

	public void update()
	{
		numSelected = 0;
		boolean unknown = false;
		for (MoleculePropertySet set : selector.getSelected())
		{
			if (set.isSizeDynamic() && !set.isComputed(dataset))
				unknown = true;
			else
				numSelected += set.getSize(dataset);
		}
		numFeaturesLabel.setText("Number of selected features: " + numSelected + (unknown ? " + ?" : ""));
		if (unknown)
			numSelected = Integer.MAX_VALUE;
		selectedFeatureType = MoleculePropertySetUtil.getType(selector.getSelected());
	}

	public int getNumSelectedFeatures()
	{
		return numSelected;
	}

	public Type getSelectedFeatureType()
	{
		return selectedFeatureType;
	}

	public void updateIntegratedFeatures(DatasetFile dataset)
	{
		if (dataset.equals(this.dataset))
			return;

		this.dataset = dataset;
		moleculePropertyPanel.setDataset(dataset);

		selfUpdate = true;

		MoleculePropertySet[] selected = selector.getSelected();
		selector.clearElements();

		IntegratedProperty[] integrated = dataset.getIntegratedProperties(true);

		selector.addElementList(INTEGRATED_FEATURES, integrated);
		selector.addElementList(CDK_FEATURES, CDKProperty.NUMERIC_DESCRIPTORS);
		//		selector.addElementList(OB_FINGERPRINT_FEATURES, OBFingerprintProperty.FINGERPRINTS);
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

			IntegratedProperty p = IntegratedProperty.fromString(feat, t);
			selector.setSelected(p);
		}

		String cdkFeatures = (String) Settings.PROPS.get("features-cdk");
		selection = VectorUtil.fromCSVString(cdkFeatures);
		for (String string : selection)
		{
			CDKDescriptor d = CDKDescriptor.fromString(string);
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
	public boolean canProceed()
	{
		return true;
	}

	@Override
	public String getTitle()
	{
		return "Extract Features";
	}

	public FeatureComputer getFeatureComputer()
	{
		return new DefaultFeatureComputer(selector.getSelected());
	}

	@Override
	public String getDescription()
	{
		return "Features may already be included in the dataset, or can be created. The features are used for the Clustering and/or 3D Embeding.";
	}

	public StructuralFragmentPropertiesPanel getFragmentPropPanel()
	{
		return fragmentProperties;
	}

}
