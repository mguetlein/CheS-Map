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
import data.CDKService;
import data.SDFProperty;

public class FeatureSelectionWizardPanel extends WizardPanel
{
	MouseOverCheckBoxList sdfList;
	DefaultListModel sdfModel;
	MouseOverCheckBoxListComponent sdfPanel;

	MouseOverCheckBoxList cdkList;
	DefaultListModel cdkModel;
	MouseOverCheckBoxListComponent cdkPanel;

	JLabel numFeaturesLabel;

	String dataset = "";
	int numSelected;
	boolean selfUpdate;

	public FeatureSelectionWizardPanel(final CheSMapperWizard wizard)
	{
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("min:grow"));
		numFeaturesLabel = new JLabel();

		builder.append("Numeric features included in dataset:");
		builder.nextLine();

		sdfModel = new DefaultListModel();
		sdfList = new MouseOverCheckBoxList(sdfModel);
		sdfList.getCheckBoxSelection().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update();
				if (!selfUpdate)
					wizard.update();
			}
		});
		sdfList.setVisibleRowCount(5);
		sdfPanel = new MouseOverCheckBoxListComponent(sdfList);

		//builder.appendRow("fill:pref");
		builder.append(sdfPanel);
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
		numSelected = sdfList.getCheckBoxSelection().getNumSelected() + cdkList.getCheckBoxSelection().getNumSelected();
		numFeaturesLabel.setText("Number of selected features: " + numSelected);
	}

	public int getNumSelectedFeatures()
	{
		return numSelected;
	}

	public void updateSDFFeatures(String dataset)
	{
		if (dataset.equals(this.dataset))
			return;
		this.dataset = dataset;
		selfUpdate = true;
		sdfList.getCheckBoxSelection().clearSelection();
		sdfModel.clear();
		SDFProperty props[] = CDKService.getNumericSDFProperties(dataset);
		for (SDFProperty sdfProperty : props)
			sdfModel.addElement(sdfProperty);

		String sdfFeatures = (String) Settings.PROPS.get("features-sdf");
		Vector<String> selection = VectorUtil.fromCSVString(sdfFeatures);
		for (String string : selection)
		{
			SDFProperty p = new SDFProperty(string);
			if (sdfModel.contains(p))
				sdfList.setCheckboxSelectedValue(p);
		}
		selfUpdate = false;
	}

	@Override
	public void proceed()
	{
		Settings.PROPS.put("features-sdf", ArrayUtil.toCSVString(sdfList.getCheckboxSelectedValues()));
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
		return new CDKFeatureComputer(ArrayUtil.cast(SDFProperty.class, sdfList.getCheckboxSelectedValues()),
				ArrayUtil.cast(CDKDescriptor.class, cdkList.getCheckboxSelectedValues()));
	}

	@Override
	public String getDescription()
	{
		return "Features may already be included in the dataset, or can be created with CDK. The features are used for the Clustering and/or 3D-Embeding.";
	}

}
