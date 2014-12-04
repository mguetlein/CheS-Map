package org.chesmapper.map.property;

import java.io.File;
import java.util.Properties;
import java.util.Set;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.data.fragments.FragmentProperties;
import org.chesmapper.map.dataInterface.CompoundPropertySet;
import org.chesmapper.map.dataInterface.CompoundPropertySet.Type;
import org.chesmapper.map.dataInterface.FragmentProperty.SubstructureType;
import org.chesmapper.map.main.BinHandler;
import org.chesmapper.map.main.PropHandler;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.workflow.FeatureMappingWorkflowProvider;
import org.mg.javalib.gui.Selector;
import org.mg.javalib.gui.binloc.Binary;
import org.mg.javalib.gui.property.Property;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.ListUtil;

public class PropertySetProvider implements FeatureMappingWorkflowProvider
{
	public static enum PropertySetShortcut
	{
		integrated, cdk, ob, obFP2, obFP3, obFP4, obMACCS, fminer, benigniBossa, cdkFunct
	}

	public static PropertySetProvider INSTANCE = new PropertySetProvider();

	private PropertySetProvider()
	{
	}

	private PropertySetCategory integrated = new IntegratedPropertySetCategory();

	private PropertySetCategory cdk = new CDKFeaturesCategory();

	private PropertySetCategory pcFeatures = new PropertySetCategory("pc", new PropertySetCategory[] { cdk,
			new OBFeatureCategory() });

	private PropertySetCategory fragments = new StructuralFragmentsCategory();

	private PropertySetCategory hashedFPs = new HashedFPsCategory();

	private PropertySetCategory root = new PropertySetCategory("root", new PropertySetCategory[] { integrated,
			pcFeatures, fragments, hashedFPs });

	public PropertySetCategory getRoot()
	{
		return root;
	}

	public void clearComputedProperties(DatasetFile d)
	{
		root.clearComputedProperties(d);
	}

	public PropertySetCategory getIntegratedCategory()
	{
		return integrated;
	}

	public PropertySetCategory getStructuralFragmentCategory()
	{
		return fragments;
	}

	public PropertySetCategory getCDKCategory()
	{
		return cdk;
	}

	public void addToSelector(Selector<PropertySetCategory, CompoundPropertySet> selector, DatasetFile dataset)
	{
		for (PropertySetCategory cat : getRoot().getSubCategory())
			recursiveAddToSelector(selector, dataset, cat, new PropertySetCategory[0]);

		for (PropertySetCategory cat : getRoot().getSubCategory())
			if (cat.getSubCategory() != null)
				selector.expand(cat);
	}

	private void recursiveAddToSelector(Selector<PropertySetCategory, CompoundPropertySet> selector,
			DatasetFile dataset, PropertySetCategory cat, PropertySetCategory... parents)
	{
		if (cat.getSubCategory() != null)
			for (PropertySetCategory subCat : cat.getSubCategory())
				recursiveAddToSelector(selector, dataset, subCat,
						ArrayUtil.push(PropertySetCategory.class, parents, cat));
		else
		{
			CompoundPropertySet set[] = cat.getPropertySet(dataset);
			set = ArrayUtil.filter(CompoundPropertySet.class, set, new ListUtil.Filter<CompoundPropertySet>()
			{
				@Override
				public boolean accept(CompoundPropertySet p)
				{
					return !p.isHiddenFromGUI();
				}
			});
			selector.addElementList(ArrayUtil.push(PropertySetCategory.class, parents, cat), set);
		}
	}

	public void putToProperties(CompoundPropertySet[] selected, Properties props, DatasetFile dataset)
	{
		root.putToProperties(selected, props, dataset);
		for (Property p : FragmentProperties.getProperties())
			p.put(props);
	}

	public void loadFromProperties(Properties props, boolean storeToSettings, DatasetFile dataset)
	{
		root.loadFromProperties(props, storeToSettings, dataset);
	}

	public CompoundPropertySet[] getDescriptorSets(DatasetFile dataset,
			PropertySetProvider.PropertySetShortcut... shortcuts)
	{
		return root.getPropertySetChildren(dataset, shortcuts);
	}

	@Override
	public CompoundPropertySet[] getFeaturesFromMappingWorkflow(Properties props, boolean storeToSettings,
			DatasetFile dataset)
	{
		for (Property p : FragmentProperties.getProperties())
			p.loadOrResetToDefault(props);

		if (storeToSettings)
			for (Property p : FragmentProperties.getProperties())
				p.put(PropHandler.getProperties());

		return root.loadFromProperties(props, storeToSettings, dataset);
	}

	@Override
	public void exportFeaturesToMappingWorkflow(CompoundPropertySet[] features, Properties props, DatasetFile dataset)
	{
		for (CompoundPropertySet feature : features)
			if (feature.getType() == null)
				throw new IllegalArgumentException("cannot export un-typed features for workflow: " + feature);
		putToProperties(features, props, dataset);
	}

	@Override
	public void exportSettingsToMappingWorkflow(Properties workflowMappingProps)
	{
		root.exportToWorkflow(workflowMappingProps);
		for (Property p : FragmentProperties.getProperties())
			p.put(workflowMappingProps);
	}

	private class MineStructuralFragmentsCategory extends PropertySetCategory
	{
		public MineStructuralFragmentsCategory()
		{
			super("struct.mine");
		}

		public boolean isFragmentCategory()
		{
			return true;
		}

		public CompoundPropertySet[] getPropertySet(DatasetFile dataset)
		{
			return ListedFragments.getSets(SubstructureType.MINE);
		}

		@Override
		public CompoundPropertySet[] getPropertySet(DatasetFile dataset,
				PropertySetProvider.PropertySetShortcut shortcut)
		{
			switch (shortcut)
			{
				case obFP2:
					return new CompoundPropertySet[] { OBFingerprintSet.getOBFingerprintSet(OBFingerprintType.FP2) };
				case fminer:
					return new CompoundPropertySet[] { FminerPropertySet.INSTANCE };
				default:
					return null;
			}
		}
	};

	private class MatchStructuralFragmentsCategory extends PropertySetCategory
	{
		public MatchStructuralFragmentsCategory()
		{
			super("struct.match");
		}

		@Override
		public String getDescriptionParam()
		{
			return Settings.STRUCTURAL_FRAGMENT_DIR + File.separator;
		}

		public boolean isSMARTSFragmentCategory()
		{
			return true;
		}

		public CompoundPropertySet[] getPropertySet(DatasetFile dataset)
		{
			return ListedFragments.getSets(SubstructureType.MATCH);
		}

		@Override
		public CompoundPropertySet[] getPropertySet(DatasetFile dataset,
				PropertySetProvider.PropertySetShortcut shortcut)
		{
			switch (shortcut)
			{
				case benigniBossa:
					return new CompoundPropertySet[] { ListedFragments
							.findFromString(ListedFragments.SMARTS_LIST_PREFIX + "ToxTree_BB_CarcMutRules") };
				case obFP3:
					return new CompoundPropertySet[] { OBFingerprintSet.getOBFingerprintSet(OBFingerprintType.FP3) };
				case obFP4:
					return new CompoundPropertySet[] { OBFingerprintSet.getOBFingerprintSet(OBFingerprintType.FP4) };
				case obMACCS:
					return new CompoundPropertySet[] { OBFingerprintSet.getOBFingerprintSet(OBFingerprintType.MACCS) };
				case cdkFunct:
					return new CompoundPropertySet[] { CDKFingerprintSet.FUNCTIONAL_GROUPS };
				default:
					return null;
			}
		}
	};

	private class StructuralFragmentsCategory extends PropertySetCategory
	{
		public StructuralFragmentsCategory()
		{
			super("struct", new PropertySetCategory[] { new MineStructuralFragmentsCategory(),
					new MatchStructuralFragmentsCategory() });
		}

		public boolean isFragmentCategory()
		{
			return true;
		}

		protected String getSerializeKey()
		{
			return "features-fragments";
		}

		protected CompoundPropertySet fromString(String s, Type t, DatasetFile dataset)
		{
			return ListedFragments.findFromString(s);
		}
	};

	private class HashedFPsCategory extends PropertySetCategory
	{
		public HashedFPsCategory()
		{
			super("hashed");
		}

		@Override
		public String getDescriptionParam()
		{
			return Settings.CDK_STRING;
		}

		public boolean isSMARTSFragmentCategory()
		{
			return false;
		}

		public CompoundPropertySet[] getPropertySet(DatasetFile dataset)
		{
			return CDKHashedFingerprintSet.FINGERPRINTS;
		}

		@Override
		protected String getSerializeKey()
		{
			return "features-hashed";
		}

		protected CompoundPropertySet fromString(String s, Type t, DatasetFile dataset)
		{
			return CDKHashedFingerprintSet.fromString(s);
		}

		@Override
		public CompoundPropertySet[] getPropertySet(DatasetFile dataset,
				PropertySetProvider.PropertySetShortcut shortcut)
		{
			return null;
		}
	};

	private class CDKFeaturesCategory extends PropertySetCategory
	{
		public CDKFeaturesCategory()
		{
			super("cdk");
		}

		@Override
		public String getDescriptionParam()
		{
			return Settings.CDK_STRING;
		}

		public PropertySetCategory[] getSubCategory()
		{
			Set<String> cdkClasses = CDKPropertySet.getNumericDescriptorClasses();
			PropertySetCategory subs[] = new PropertySetCategory[cdkClasses.size()];
			int i = 0;
			for (final String cdkClass : cdkClasses)
			{
				subs[i++] = new PropertySetCategory("cdk")
				{
					public String toString()
					{
						return cdkClass;
					}

					public CompoundPropertySet[] getPropertySet(DatasetFile dataset)
					{
						return CDKPropertySet.getNumericDescriptorsForClass(cdkClass);
					}

					@Override
					public String getDescriptionParam()
					{
						return Settings.CDK_STRING;
					}

					@Override
					public CompoundPropertySet[] getPropertySet(DatasetFile dataset,
							PropertySetProvider.PropertySetShortcut shortcut)
					{
						if (shortcut == PropertySetProvider.PropertySetShortcut.cdk)
							return ArrayUtil.filter(CompoundPropertySet.class, getPropertySet(dataset),
									new ListUtil.Filter<CompoundPropertySet>()
									{
										@Override
										public boolean accept(CompoundPropertySet s)
										{
											return !s.toString().contains("Ionization Potential");
										}
									});
						else
							return null;
					}
				};
			}
			return subs;
		}

		protected String getSerializeKey()
		{
			return "features-cdk";
		}

		protected CompoundPropertySet fromString(String s, Type t, DatasetFile dataset)
		{
			return CDKPropertySet.fromString(s);
		}

	};

	private class OBFeatureCategory extends PropertySetCategory
	{
		public OBFeatureCategory()
		{
			super("ob");
		}

		public Binary getBinary()
		{
			return BinHandler.BABEL_BINARY;
		}

		@Override
		public String getDescriptionParam()
		{
			return Settings.OPENBABEL_STRING;
		}

		public CompoundPropertySet[] getPropertySet(DatasetFile dataset)
		{
			return OBDescriptorSet.getDescriptors(false);
		}

		@Override
		public CompoundPropertySet[] getPropertySet(DatasetFile dataset,
				PropertySetProvider.PropertySetShortcut shortcut)
		{
			if (shortcut == PropertySetProvider.PropertySetShortcut.ob)
				return getPropertySet(dataset);
			else
				return null;
		}

		protected String getSerializeKey()
		{
			return "features-ob";
		}

		protected CompoundPropertySet fromString(String s, Type t, DatasetFile dataset)
		{
			return OBDescriptorSet.fromString(s, t);
		}
	};

	private class IntegratedPropertySetCategory extends PropertySetCategory
	{
		public IntegratedPropertySetCategory()
		{
			super("integrated");
		}

		public CompoundPropertySet[] getPropertySet(DatasetFile dataset)
		{
			return dataset.getIntegratedProperties();
		}

		protected String getSerializeKey()
		{
			return "features-integrated";
		}

		@Override
		protected String getSerializeKeyType()
		{
			return "feature-type";
		}

		protected CompoundPropertySet fromString(String s, Type t, DatasetFile dataset)
		{
			return IntegratedPropertySet.fromString(s, t, dataset);
		}

		@Override
		public CompoundPropertySet[] getPropertySet(DatasetFile dataset,
				PropertySetProvider.PropertySetShortcut shortcut)
		{
			if (shortcut == PropertySetProvider.PropertySetShortcut.integrated)
				return dataset.getIntegratedProperties();
			else
				return null;
		}
	}

}
