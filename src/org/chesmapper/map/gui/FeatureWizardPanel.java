package org.chesmapper.map.gui;

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
import java.util.List;

import javax.swing.AbstractAction;
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

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.FeatureLoader;
import org.chesmapper.map.data.fragments.FragmentProperties;
import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.CompoundPropertySet;
import org.chesmapper.map.dataInterface.CompoundPropertySetUtil;
import org.chesmapper.map.dataInterface.FragmentPropertySet;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.CompoundPropertySet.Type;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.PropHandler;
import org.chesmapper.map.main.ScreenSetup;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.property.ListedFragments;
import org.chesmapper.map.property.OBDescriptorSet;
import org.chesmapper.map.property.PropertySetCategory;
import org.chesmapper.map.property.PropertySetProvider;
import org.chesmapper.map.util.MessageUtil;
import org.chesmapper.map.util.WizardComponentFactory;
import org.mg.javalib.gui.LinkButton;
import org.mg.javalib.gui.Messages;
import org.mg.javalib.gui.Selector;
import org.mg.javalib.gui.WizardPanel;
import org.mg.javalib.util.DoubleImageIcon;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.ImageLoader;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class FeatureWizardPanel extends WizardPanel
{
	Selector<PropertySetCategory, CompoundPropertySet> selector;
	JLabel numFeaturesLabel;
	LinkButton loadFeaturesButton = new LinkButton("Load");
	CompoundPropertyPanel compoundPropertyPanel;
	JScrollPane propertyScroll;

	DatasetFile dataset = null;
	boolean selfUpdate;

	JPanel structuralFragmentPropsContainer;
	private JPanel fragmentPropsPanel;
	private JPanel fragmentPropsPanelSMARTS;
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

		selector.setAddButtonText("Add feature");
		selector.setRemoveButtonText("Remove feature");

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

		fragmentProperties = new StructuralFragmentPropertiesPanel(wizard);

		fragmentProperties.setBorder(new CompoundBorder(new EmptyBorder(0, 0, 10, 0), fragmentProperties.getBorder()));
		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("p,fill:p:grow"));
		addSmarts = new JButton("Add SMARTS file");

		builder.append(addSmarts);
		builder.nextLine();
		builder.appendParagraphGapRow();
		builder.nextLine();
		builder.append(fragmentProperties.getSummaryPanel(wizard), 2);
		fragmentPropsPanelSMARTS = builder.getPanel();

		fragmentPropsPanel = new JPanel();
		fragmentPropsPanel.add(fragmentProperties.getSummaryPanel(wizard));

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
		selector = new Selector<PropertySetCategory, CompoundPropertySet>(CompoundPropertySet.class,
				PropertySetProvider.INSTANCE.getRoot(), (ScreenSetup.INSTANCE.isWizardSpaceSmall() ? 6 : 12))
		{
			public Icon getIcon(CompoundPropertySet elem)
			{
				ImageIcon warningIcon = null;
				ImageIcon typeIcon = null;

				if (elem.getBinary() != null && !elem.getBinary().isFound())
					warningIcon = ImageLoader.getImage(ImageLoader.Image.error);

				Type type = CompoundPropertySetUtil.getType(elem);
				if (type == Type.NUMERIC)
					typeIcon = ImageLoader.getImage(ImageLoader.Image.numeric);
				else if (type == Type.NOMINAL)
					typeIcon = ImageLoader.getImage(ImageLoader.Image.distinct);
				else if (elem.getSize(dataset) == 1 && warningIcon == null)
					warningIcon = ImageLoader.getImage(ImageLoader.Image.warning);

				if (!elem.isComputed(dataset) && !(Settings.CACHING_ENABLED && elem.isCached(dataset))
						&& elem.isComputationSlow())
					warningIcon = ImageLoader.getImage(ImageLoader.Image.hourglass);

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
			public ImageIcon getCategoryIcon(PropertySetCategory cat)
			{
				if (cat.getBinary() != null && !cat.getBinary().isFound())
					return ImageLoader.getImage(ImageLoader.Image.error);
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

			//			@Override
			//			public Dimension getPreferredSize()
			//			{
			//				Dimension dim = super.getPreferredSize();
			//				return new Dimension(dim.width, dim.height + 45);
			//			}
		};
	}

	private void addListeners()
	{
		BinHandler.BABEL_BINARY.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (wizard.isClosed())
					return;
				if (BinHandler.BABEL_BINARY.isFound())
				{
					OBDescriptorSet.loadDescriptors(true);
					updateFeatures(dataset, true);
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
				JOptionPane
						.showMessageDialog(
								Settings.TOP_LEVEL_FRAME,
								"You have no feature/s selected. Please select (a group of) feature/s in the left panel before clicking 'Add feature'.",
								"Warning - No feature/s selected", JOptionPane.WARNING_MESSAGE);
			}
		});
		selector.addPropertyChangeListener(Selector.PROPERTY_EMPTY_REMOVE, new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				JOptionPane
						.showMessageDialog(
								Settings.TOP_LEVEL_FRAME,
								"You have no feature/s selected. Please select (a group of) feature/s in the right panel before clicking 'Remove feature'.",
								"Warning - No feature/s selected", JOptionPane.WARNING_MESSAGE);
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
				if (wizard.isClosed())
					return;
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
					ListedFragments.reset(name);
					updateFeatures(dataset, true);
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

		ListedFragments.init(); // load instances first (listener order is important)
		FragmentProperties.addPropertyChangeListenerToProperties(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (wizard.isClosed())
					return;
				update();
			}
		}, false);
	}

	protected void updateFeatureProperties(PropertySetCategory highlightedCategory)
	{
		String info = null;
		JPanel props = null;

		if (highlightedCategory == null)
			info = PropertySetProvider.INSTANCE.getRoot().getDescription();
		else
		{
			info = highlightedCategory.getDescription();
			if (highlightedCategory.isFragmentCategory())
				props = fragmentPropsPanel;
			else if (highlightedCategory.isSMARTSFragmentCategory())
				props = fragmentPropsPanelSMARTS;
			else if (highlightedCategory.getBinary() != null)
			{
				JComponent pp = BinHandler.getBinaryComponent(highlightedCategory.getBinary(), wizard);
				JPanel p = new JPanel();
				p.add(pp);
				props = p;
			}
		}

		compoundPropertyPanel.showInfoText(info);
		structuralFragmentPropsContainer.setIgnoreRepaint(true);
		structuralFragmentPropsContainer.removeAll();
		if (props != null)
			structuralFragmentPropsContainer.add(props, BorderLayout.WEST);
		structuralFragmentPropsContainer.setIgnoreRepaint(false);
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

		featureInfo = new FeatureInfo(dataset, selector.getSelected());
		numFeaturesLabel.setText("Number of selected features: " + featureInfo.numFeatures
				+ (featureInfo.numFeaturesUnknown ? " + ?" : ""));

		boolean notComputed = false;
		boolean slowMiningFeatureSelected = false;
		boolean slowFeaturesSelected = false;
		for (CompoundPropertySet set : selector.getSelected())
		{
			if (!set.isComputed(dataset))
				notComputed = true;
			if (!set.isComputed(dataset) && !(Settings.CACHING_ENABLED && set.isCached(dataset))
					&& set.isComputationSlow())
			{
				slowFeaturesSelected = true;
				if (set instanceof FragmentPropertySet && set.isSizeDynamic())
					slowMiningFeatureSelected = true;
			}
		}
		msg = null;
		if (slowMiningFeatureSelected)
			msg = Messages.slowMessage(Settings.text("features.slowMining"), new AbstractAction(
					"Settings for fragments")
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					fragmentProperties.showDialog(wizard);
				}
			});
		else if (slowFeaturesSelected)
			msg = MessageUtil.slowRuntimeMessages(Settings.text("features.slow"));
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
		private int numFeatures = 0;
		private int numCompounds = 0;
		private boolean numFeaturesUnknown = false;
		private boolean numUnknwonFeaturesHigh = false;

		private boolean featuresSelected = false;
		private boolean numericFeature = false;
		private boolean nonBinaryNominalFeature = false;
		private boolean fragmentFeature = false;

		public FeatureInfo(DatasetFile d, CompoundPropertySet sets[])
		{
			numCompounds = (d == null ? 0 : d.numCompounds());
			for (CompoundPropertySet set : sets)
			{
				featuresSelected = true;
				if (set.getType() == Type.NUMERIC)
					numericFeature = true;
				else if (set.getType() == Type.NOMINAL)
				{
					if (set instanceof FragmentPropertySet)
						fragmentFeature = true;
					else
						for (int i = 0; i < set.getSize(d); i++)
							if (set.isComputed(d) && ((NominalProperty) set.get(d, i)).getDomain().length > 2)
								nonBinaryNominalFeature = true;
				}
				else
					throw new IllegalStateException();

				if (set.isSizeDynamic() && !set.isComputed(d))
				{
					numFeaturesUnknown = true;
					if (set.isSizeDynamicHigh(d))
						numUnknwonFeaturesHigh = true;
				}
				else
				{
					int num = set.getSize(d);
					numFeatures += num;
				}
			}
		}

		public boolean isFeaturesSelected()
		{
			return featuresSelected;
		}

		public boolean isFragmentFeatureSelected()
		{
			return fragmentFeature;
		}

		public boolean isNumericFeatureSelected()
		{
			return numericFeature;
		}

		public boolean isOnlyBinaryFeaturesSelected()
		{
			if (!featuresSelected)
				throw new IllegalStateException();
			return !numericFeature && !nonBinaryNominalFeature;
		}

		public boolean isOnlyNominalFeaturesSelected()
		{
			if (!featuresSelected)
				throw new IllegalStateException();
			return !numericFeature;
		}

		public boolean isNumPairsHigh()
		{
			if (numFeatures * numCompounds >= 250000)
				return true;
			if (numFeaturesUnknown)
				return numUnknwonFeaturesHigh;
			return false;
		}

		public String getNumPairsWarning()
		{
			if (!isNumPairsHigh())
				throw new IllegalStateException();
			String msg = "The selected algorithm could run a long time because the number of compound feature pairs is ";
			if (numFeatures * numCompounds >= 250000)
			{
				msg += "high (" + (numFeaturesUnknown ? ">=" : "") + (numFeatures * numCompounds / 1000) + "k).";
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

	public void updateFeatures(DatasetFile dataset)
	{
		updateFeatures(dataset, false);
	}

	private void updateFeatures(DatasetFile dataset, boolean forceUpdate)
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

		PropertySetProvider.INSTANCE.addToSelector(selector, dataset);

		for (CompoundPropertySet m : PropertySetProvider.INSTANCE.getFeaturesFromMappingWorkflow(
				PropHandler.getProperties(), false, dataset))
			selector.setSelected(m, false);

		selector.setSelected(selected, false);

		update();
		selfUpdate = false;
	}

	@Override
	public void proceed()
	{
		PropertySetProvider.INSTANCE.putToProperties(selector.getSelected(), PropHandler.getProperties(), dataset);

		PropHandler.storeProperties();
		if (fragmentProperties != null)
			fragmentProperties.store();
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

}
