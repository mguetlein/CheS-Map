package gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
import util.ListUtil;
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
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import dataInterface.MoleculePropertySet;
import dataInterface.MoleculePropertySetUtil;

public class FeatureSelectionWizardPanel extends WizardPanel
{
	Selector<MoleculePropertySet> selector;

	JLabel numFeaturesLabel;

	MoleculePropertyPanel moleculePropertyPanel;

	DatasetFile dataset = null;
	int numSelected;
	boolean selfUpdate;

	public static final String INTEGRATED_FEATURES = "Included in Dataset";
	public static final String CDK_FEATURES = "CDK descriptors (" + Settings.CDK_STRING + ")";
	public static final String OB_FINGERPRINT_FEATURES = "OpenBabel Fingerprints";

	public FeatureSelectionWizardPanel(final CheSMapperWizard wizard)
	{
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("min:grow"));
		numFeaturesLabel = new JLabel();

		JPanel lPanel = new JPanel(new GridLayout(0, 2, 10, 10));
		lPanel.add(new JLabel("Available Features:"));
		lPanel.add(new JLabel("Selected Features:"));
		builder.append(lPanel);
		builder.nextLine();

		selector = new Selector<MoleculePropertySet>(MoleculePropertySet.class, "Features")
		{
			public ImageIcon getIcon(MoleculePropertySet elem)
			{
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
				return MoleculePropertySetUtil.getType(elem) != null;
			}

			@Override
			public ImageIcon getCategoryIcon(String name)
			{
				return null;
			}
		};

		selector.setAddButtonText("Add feature");
		selector.setRemoveButtonText("Remove feature");
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
				moleculePropertyPanel.setSelectedPropertySet(selector.getHighlightedElement());
			}
		});
		selector.addPropertyChangeListener(Selector.PROPERTY_TRY_ADDING_INVALID, new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				JOptionPane.showMessageDialog(
						Settings.TOP_LEVEL_COMPONENT,
						"Could not add the following feature(s):\n"
								+ ListUtil.toString((List<?>) evt.getNewValue(), "\n")
								+ "\n\nThe feature(s) is/are most likely not suited for clustering and embedding.\nYou have to asign the feature type manually (by clicking on 'Nominal') before adding the feature/s.",
						"Warning - Could not add feature", JOptionPane.WARNING_MESSAGE);
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

		setLayout(new BorderLayout());
		add(builder.getPanel());
	}

	int selectedPropertyIndex = 0;
	List<MoleculeProperty> selectedProperties = new ArrayList<MoleculeProperty>();

	public void update()
	{
		numSelected = selector.getSelected().length;
		numFeaturesLabel.setText("Number of selected features: " + numSelected);
	}

	public int getNumSelectedFeatures()
	{
		return numSelected;
	}

	public void updateIntegratedFeatures(DatasetFile dataset)
	{
		if (dataset.equals(this.dataset))
			return;

		this.dataset = dataset;
		moleculePropertyPanel.setDataset(dataset);

		selfUpdate = true;

		selector.clearElements();

		IntegratedProperty[] integrated = dataset.getIntegratedProperties(true);

		selector.addElementList(INTEGRATED_FEATURES, integrated);
		selector.addElementList(CDK_FEATURES, CDKProperty.NUMERIC_DESCRIPTORS);
		if (Settings.CV_BABEL_PATH != null)
			selector.addElementList(OB_FINGERPRINT_FEATURES, OBFingerprintProperty.FINGERPRINTS);

		String integratedFeatures = (String) Settings.PROPS.get("features-integrated");
		Vector<String> selection = VectorUtil.fromCSVString(integratedFeatures);
		for (String string : selection)
		{
			IntegratedProperty p = IntegratedProperty.fromString(string);
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

		update();

		selfUpdate = false;
	}

	@Override
	public void proceed()
	{
		Settings.PROPS.put("features-integrated", ArrayUtil.toCSVString(selector.getSelected(INTEGRATED_FEATURES)));
		Settings.PROPS.put("features-cdk", ArrayUtil.toCSVString(selector.getSelected(CDK_FEATURES)));
		Settings.PROPS.put("features-ob-fingerprints",
				ArrayUtil.toCSVString(selector.getSelected(OB_FINGERPRINT_FEATURES)));
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
		return "Features may already be included in the dataset, or can be created with CDK. The features are used for the Clustering and/or 3D-Embeding.";
	}

}
