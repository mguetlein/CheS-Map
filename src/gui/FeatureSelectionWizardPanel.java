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
import data.OBFingerprintProperty;
import data.OBFingerprintProperty.OBFingerPrints;
import data.StructuralAlerts;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import dataInterface.MoleculePropertySet;
import dataInterface.MoleculePropertySetUtil;

public class FeatureSelectionWizardPanel extends WizardPanel
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
	public static final String CDK_FEATURES = "CDK descriptors";
	public static final String OB_FINGERPRINT_FEATURES = "OpenBabel Fingerprints";
	public static final String STRUCTURAL_ALERTS = "Structural Alerts";

	public FeatureSelectionWizardPanel(final CheSMapperWizard wizard)
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
					selector.repaint();
			}
		});

		selector = new Selector<MoleculePropertySet>(MoleculePropertySet.class, ROOT)
		{
			public ImageIcon getIcon(MoleculePropertySet elem)
			{
				if (elem instanceof OBFingerprintProperty.OBFingerPrints && !Settings.BABEL_BINARY.isFound())
					return ImageLoader.ERROR;

				MoleculeProperty.Type type = MoleculePropertySetUtil.getType(elem);
				if (type == Type.NUMERIC)
					return ImageLoader.NUMERIC;
				else if (type == Type.NOMINAL)
					return ImageLoader.DISTINCT;
				else
				{
					if (elem.getSize() == 1)
						return ImageLoader.WARNING;
					else
						return null;
				}
			}

			@Override
			public boolean isValid(MoleculePropertySet elem)
			{
				if (elem instanceof OBFingerprintProperty.OBFingerPrints && !Settings.BABEL_BINARY.isFound())
					return false;
				return MoleculePropertySetUtil.getType(elem) != null;
			}

			@Override
			public ImageIcon getCategoryIcon(String name)
			{
				if (OB_FINGERPRINT_FEATURES.equals(name) && !Settings.BABEL_BINARY.isFound())
					return ImageLoader.ERROR;
				return null;
			}

			@Override
			public String getString(MoleculePropertySet elem)
			{
				if (elem.getSize() == 1)
					return elem.toString();
				else
					return elem + " (" + elem.getSize() + " features)";
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
				if (set.get(0) instanceof OBFingerprintProperty.OBFingerPrints)
					b.append(Settings.getBinaryComponent(Settings.BABEL_BINARY));
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

		builder.appendParagraphGapRow();
		builder.nextLine();

		builder.appendSeparator("Feature properties");
		builder.nextLine();

		moleculePropertyPanel = new MoleculePropertyPanel();
		moleculePropertyPanel.addPropertyChangeListener(MoleculePropertyPanel.PROPERTY_TYPE_CHANGED,
				new PropertyChangeListener()
				{
					@Override
					public void propertyChange(PropertyChangeEvent evt)
					{
						selector.repaint();
					}
				});

		builder.append(moleculePropertyPanel);
		builder.nextLine();

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
		String info = "The available features are shown in the left panel. Select (a group of) feature/s and click '"
				+ addFeaturesText
				+ "'. The selected features - shown in the right panel - will be used for clustering and/or embedding.\n\n"
				+ "The clustering/embedding result relies on the selected features. For example, select structural features (e.g. OpenBabel Fingerprint FP2) to cluster structural similar compounds together and to place structural similar compounds close together in 3D-space.\n\n"
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
			else if (highlightedCategory.equals(OB_FINGERPRINT_FEATURES))
			{
				info = "Uses "
						+ Settings.OPENBABEL_STRING
						+ ".\n\n"
						+ "OpenBabel provides several Fingerprints that can be computed for each comopund. Each bit of the fingerprint is converted to a binary nominal feature.";
				bin = Settings.BABEL_BINARY;
			}
			else if (highlightedCategory.equals(STRUCTURAL_ALERTS))
				info = "Structural alerts are smarts that define a molecular subgraph."
						+ "\nEach alert is used as a binary nominal feature (1 => subgraph occurs, 0 => subgraph does not occur)."
						+ "\n\nCopy a smarts.csv into the following folder to integrate any structural alerts: "
						+ Settings.STRUCTURAL_ALERTS_DIR + File.separator
						+ "\nEach line in the csv-file should look like this:"
						+ "\n\"alert\",\"description\",\"smarts\",[optional: more smarts - only one has to match]"
						+ "\nComments (starting with '#') will be printed as description. Example csv file:\n"
						+ "\n\"Benzene\",\"Most used example fragment\",\"c1ccccc1\""
						+ "\n\"Carbonyl with Carbon\",,\"[CX3](=[OX1])C\""
						+ "\n\"Carbonyl with Nitrogen or Oxygen\",,\"[OX1]=CN\",\"[CX3](=[OX1])O\"";
			else
				info = StructuralAlerts.instance.getDescriptionForName(highlightedCategory);
		}
		moleculePropertyPanel.showInfoText(info);

		if (bin == null)
			binaryPanelContainer.removeAll();
		else
			binaryPanelContainer.add(Settings.getBinaryComponent(bin), BorderLayout.WEST);
		binaryPanelContainer.revalidate();
		binaryPanelContainer.repaint();
	}

	int selectedPropertyIndex = 0;
	List<MoleculeProperty> selectedProperties = new ArrayList<MoleculeProperty>();

	public void update()
	{
		numSelected = 0;
		for (MoleculePropertySet set : selector.getSelected())
			numSelected += set.getSize();
		numFeaturesLabel.setText("Number of selected features: " + numSelected);

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
		selector.addElementList(OB_FINGERPRINT_FEATURES, OBFingerprintProperty.FINGERPRINTS);
		selector.addElements(STRUCTURAL_ALERTS);
		for (int i = 0; i < StructuralAlerts.instance.getNumSets(); i++)
		{
			selector.addElementList(new String[] { STRUCTURAL_ALERTS, StructuralAlerts.instance.getSetName(i) },
					StructuralAlerts.instance.getSet(i));
		}

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

		String obFeatures = (String) Settings.PROPS.get("features-ob-fingerprints");
		selection = VectorUtil.fromCSVString(obFeatures);
		for (String string : selection)
		{
			OBFingerPrints d = OBFingerPrints.fromString(string);
			selector.setSelected(d);
		}

		String alertFeatures = (String) Settings.PROPS.get("features-alerts");
		selection = VectorUtil.fromCSVString(alertFeatures);
		for (String string : selection)
		{
			StructuralAlerts.Alert d = StructuralAlerts.instance.findFromString(string);
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
			serilizedProps[i] = integratedProps[i] + "#" + integratedProps[i].get(0).getType();
		Settings.PROPS.put("features-integrated", ArrayUtil.toCSVString(serilizedProps, true));

		Settings.PROPS.put("features-cdk", ArrayUtil.toCSVString(selector.getSelected(CDK_FEATURES), true));
		Settings.PROPS.put("features-ob-fingerprints",
				ArrayUtil.toCSVString(selector.getSelected(OB_FINGERPRINT_FEATURES), true));
		Settings.PROPS.put("features-alerts", ArrayUtil.toCSVString(selector.getSelected(STRUCTURAL_ALERTS), true));
		Settings.storeProps();
	}

	@Override
	public boolean canProceed()
	{
		return true;
	}

	@Override
	public String getTitle()
	{
		return "Select features";
	}

	public FeatureComputer getFeatureComputer()
	{
		return new DefaultFeatureComputer(selector.getSelected());
	}

	@Override
	public String getDescription()
	{
		return "Features may already be included in the dataset, or can be created. The features are used for the Clustering and/or 3D-Embeding.";
	}

}
