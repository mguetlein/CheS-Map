package gui;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;

import main.Settings;
import util.ArrayUtil;
import util.VectorUtil;
import alg.FeatureComputer;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import data.CDKFeatureComputer;
import data.CDKProperty;
import data.CDKProperty.CDKDescriptor;
import data.DatasetFile;
import data.IntegratedProperty;

public class FeatureSelectionWizardPanel extends WizardPanel
{
	MouseOverCheckBoxList integratedList;
	DefaultListModel integratedModel;
	MouseOverCheckBoxListComponent integratedPanel;

	MouseOverCheckBoxList cdkList;
	DefaultListModel cdkModel;
	MouseOverCheckBoxListComponent cdkPanel;

	JLabel numFeaturesLabel;

	DatasetFile dataset = null;
	int numSelected;
	boolean selfUpdate;

	public FeatureSelectionWizardPanel(final CheSMapperWizard wizard)
	{
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("min:grow"));
		numFeaturesLabel = new JLabel();

		builder.append("Numeric features included in dataset:");
		builder.nextLine();

		integratedModel = new DefaultListModel();
		integratedList = new MouseOverCheckBoxList(integratedModel);
		integratedList.getCheckBoxSelection().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update();
				if (!selfUpdate)
					wizard.update();
			}
		});
		integratedList.setVisibleRowCount(5);
		integratedPanel = new MouseOverCheckBoxListComponent(integratedList);

		//builder.appendRow("fill:pref");
		builder.append(integratedPanel);
		builder.nextLine();

		builder.appendParagraphGapRow();
		builder.nextLine();

		builder.append("Compute features using " + Settings.CDK_STRING + ":");
		builder.nextLine();

		cdkModel = new DefaultListModel();
		cdkList = new MouseOverCheckBoxList(cdkModel);
		cdkList.setVisibleRowCount(5);
		cdkList.getCheckBoxSelection().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update();
				if (!selfUpdate)
					wizard.update();
			}
		});
		cdkPanel = new MouseOverCheckBoxListComponent(cdkList);

		for (CDKDescriptor desc : CDKProperty.CDK_NUMERIC_DESCRIPTORS)
			cdkModel.addElement(desc);

		String cdkFeatures = (String) Settings.PROPS.get("features-cdk");
		Vector<String> selection = VectorUtil.fromCSVString(cdkFeatures);
		for (String string : selection)
		{
			CDKDescriptor d = CDKDescriptor.valueOf(string);
			if (cdkModel.contains(d))
				cdkList.setCheckboxSelectedValue(d);
		}
		builder.appendRow("pref");
		builder.append(cdkPanel);
		builder.nextLine();

		//		builder.appendRow("fill:pref:grow");
		//		builder.append(new Panel());
		//		builder.nextLine();

		builder.appendParagraphGapRow();
		builder.nextLine();

		builder.appendSeparator("Feature properties");
		builder.nextLine();

		builder.append(numFeaturesLabel);
		builder.nextLine();

		setLayout(new BorderLayout());
		add(builder.getPanel());
	}

	public void update()
	{
		numSelected = integratedList.getCheckBoxSelection().getNumSelected()
				+ cdkList.getCheckBoxSelection().getNumSelected();
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
		selfUpdate = true;
		integratedList.getCheckBoxSelection().clearSelection();
		integratedModel.clear();
		IntegratedProperty props[] = dataset.getIntegratedNumericProperties();
		for (IntegratedProperty integratedProperty : props)
			integratedModel.addElement(integratedProperty);

		String integratedFeatures = (String) Settings.PROPS.get("features-integrated");
		Vector<String> selection = VectorUtil.fromCSVString(integratedFeatures);
		for (String string : selection)
		{
			IntegratedProperty p = new IntegratedProperty(string);
			if (integratedModel.contains(p))
				integratedList.setCheckboxSelectedValue(p);
		}
		selfUpdate = false;
	}

	@Override
	public void proceed()
	{
		Settings.PROPS.put("features-integrated", ArrayUtil.toCSVString(integratedList.getCheckboxSelectedValues()));
		Settings.PROPS.put("features-cdk", ArrayUtil.toCSVString(cdkList.getCheckboxSelectedValues()));
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
		return new CDKFeatureComputer(ArrayUtil.cast(IntegratedProperty.class,
				integratedList.getCheckboxSelectedValues()), ArrayUtil.cast(CDKDescriptor.class,
				cdkList.getCheckboxSelectedValues()));
	}

	@Override
	public String getDescription()
	{
		return "Features may already be included in the dataset, or can be created with CDK. The features are used for the Clustering and/or 3D-Embeding.";
	}

}
