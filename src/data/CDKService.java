package data;

import gui.Progressable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.vecmath.Point3d;

import main.Settings;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.iterator.IteratingMDLReader;
import org.openscience.cdk.modeling.builder3d.ModelBuilder3D;
import org.openscience.cdk.modeling.builder3d.TemplateHandler3D;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.result.DoubleArrayResult;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.IDescriptorResult;
import org.openscience.cdk.qsar.result.IntegerArrayResult;
import org.openscience.cdk.qsar.result.IntegerResult;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import util.ArrayUtil;
import util.CollectionUtil;
import data.CDKProperty.CDKDescriptor;
import dataInterface.MoleculeProperty;

public class CDKService
{
	private static HashMap<String, IMolecule[]> sdfToMolecules = new HashMap<String, IMolecule[]>();
	private static HashMap<String, Boolean> sdfHas3D = new HashMap<String, Boolean>();
	private static HashMap<String, Set<SDFProperty>> sdfProperties = new HashMap<String, Set<SDFProperty>>();
	private static HashMap<String, Object[]> values = new HashMap<String, Object[]>();
	private static SDFProperty SDFSmiles;
	private static String allCDKDescriptors[];
	static
	{
		CDKDescriptor p[] = CDKDescriptor.values();
		allCDKDescriptors = new String[p.length];
		for (int i = 0; i < p.length; i++)
			allCDKDescriptors[i] = p[i].toString();
	}

	public static SDFProperty[] getSDFProperties(String f)
	{
		loadSdf(f);
		SDFProperty p[] = new SDFProperty[sdfProperties.get(f).size()];
		sdfProperties.get(f).toArray(p);
		return p;
	}

	public static SDFProperty[] getNumericSDFProperties(String f)
	{
		loadSdf(f);
		List<SDFProperty> props = new ArrayList<SDFProperty>();
		for (SDFProperty sdfProperty : sdfProperties.get(f))
			if (sdfProperty.isNumeric())
				props.add(sdfProperty);
		SDFProperty p[] = new SDFProperty[props.size()];
		props.toArray(p);
		return p;
	}

	private static String valuesKey(String file, MoleculeProperty property)
	{
		return valuesKey(file, property, false);
	}

	private static String valuesKey(String file, MoleculeProperty property, boolean normalized)
	{
		return file + "_" + property.toString() + "_" + normalized + "_" + property.getClass().getSimpleName();
	}

	public static boolean isLoaded(String f)
	{
		return (sdfToMolecules.get(f) != null);
	}

	/**
	 * 
	 * @return num compounds
	 */
	public static int loadSdf(String f)
	{
		return loadSdf(f, true, null);
	}

	public static int loadSdf(String f, final boolean loadHydrogen)
	{
		return loadSdf(f, loadHydrogen, null);
	}

	public static void clear(String f)
	{
		if (sdfToMolecules.get(f) != null)
		{
			sdfToMolecules.remove(f);
			sdfHas3D.remove(f);
			sdfProperties.remove(f);
			//			numericSdfProperties.remove(f);
			List<String> toDel = new ArrayList<String>();
			for (String k : values.keySet())
				if (k.startsWith(f))
					toDel.add(k);
			for (String k : toDel)
				values.remove(k);
		}
	}

	public synchronized static int loadSdf(String f, boolean loadHydrogen, final Progressable progress)
	{
		if (sdfToMolecules.get(f) == null)
		{
			System.out.print("read sdf file '" + f + "' ");

			Vector<IMolecule> mols = new Vector<IMolecule>();
			sdfProperties.put(f, new HashSet<SDFProperty>());
			//			numericSdfProperties.put(f, new HashSet<SDFProperty>());

			int mCount = 0;
			try
			{
				File ff = new File(f);
				if (!ff.exists())
					throw new IllegalArgumentException("file not found: " + f);

				CDKHydrogenAdder ha = CDKHydrogenAdder.getInstance(DefaultChemObjectBuilder.getInstance());
				IteratingMDLReader reader = new IteratingMDLReader(new InputStreamReader(new FileInputStream(ff)),
						DefaultChemObjectBuilder.getInstance());

				while (reader.hasNext())
				{
					System.out.print(".");

					IMolecule mol = (IMolecule) reader.next();

					if (progress != null)
					{
						final int finalMCount = mCount;
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								progress.update(finalMCount + 1, "Loaded " + (finalMCount + 1) + " molecules");
							}
						});
					}

					Map<Object, Object> props = mol.getProperties();
					for (Object key : props.keySet())
					{
						//						if (ArrayUtil.indexOf(allCDKDescriptors, key.toString()) != -1)
						//							throw new IllegalStateException("sdf-property has equal name as cdk-descriptor: "
						//									+ key.toString());
						SDFProperty p = new SDFProperty(key.toString());
						// add key to sdfProperties
						sdfProperties.get(f).add(p);

						if (key.toString().equals("STRUCTURE_SMILES"))
							SDFSmiles = p;

						// add value to values
						String valuesKey = valuesKey(f, p);
						if (!values.containsKey(valuesKey))
							values.put(valuesKey, new Object[] { props.get(key) });
						else
						{
							Object o[] = Arrays.copyOf(values.get(valuesKey), mCount + 1);
							o[mCount] = props.get(key);
							values.put(valuesKey, o);
						}
					}

					try
					{
						if (loadHydrogen)
						{
							AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
							ha.addImplicitHydrogens(mol);
							AtomContainerManipulator.convertImplicitToExplicitHydrogens(mol);
						}

					}
					catch (CDKException e)
					{
						System.err.println("Could not add hydrogens:  " + e.getMessage());
					}

					mols.add(mol);
					mCount++;

				}
				reader.close();
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			System.out.println(" done (" + mols.size() + " molecules found)");

			// convert string to double
			for (SDFProperty p : sdfProperties.get(f))
			{
				Object o[] = values.get(valuesKey(f, p));
				if (o.length < mCount)
				{
					// add trailing nulls for missing props
					o = Arrays.copyOf(o, mCount);
					values.put(valuesKey(f, p), o);
				}
				Double d[] = ArrayUtil.parse(o);
				if (d != null)
				{
					//					numericSdfProperties.get(f).add(p);
					p.setNumeric(true);
					values.put(valuesKey(f, p), d);
					values.put(valuesKey(f, p, true), ArrayUtil.normalize(d));
				}
				else
				{
					// normalization of string elements
					values.put(valuesKey(f, p, true), ArrayUtil.normalize(o));
				}
			}

			IMolecule res[] = new IMolecule[mols.size()];
			mols.toArray(res);
			sdfToMolecules.put(f, res);

			System.out.println("properties in sdf file: " + CollectionUtil.toString(sdfProperties.get(f)));
			//			System.out.println("               numeric: " + CollectionUtil.toString(numericSdfProperties.get(f)));

		}
		return sdfToMolecules.get(f).length;
	}

	public static IMolecule[] getMoleculeFromSdf(String f)
	{
		return getMoleculeFromSdf(f, true);
	}

	public static IMolecule[] getMoleculeFromSdf(String f, boolean loadHydrogen)
	{
		loadSdf(f, loadHydrogen);
		return sdfToMolecules.get(f);
	}

	public static String[] getStringFromSdf(String f, MoleculeProperty p)
	{
		return ArrayUtil.cast(String.class, fromSdf(f, p, false));
	}

	public static Object[] getObjectFromSdf(String f, MoleculeProperty p)
	{
		return fromSdf(f, p, false);
	}

	public static Double[] getFromSdf(String f, MoleculeProperty p, boolean normalize)
	{
		return ArrayUtil.cast(Double.class, fromSdf(f, p, normalize));
	}

	private static Object[] fromSdf(String f, MoleculeProperty p, boolean normalize)
	{
		loadSdf(f);

		if (values.get(valuesKey(f, p, normalize)) == null)
		{
			if (p instanceof SDFProperty)
			{
				// this can happen if a cluster has only null values for a sdf property-> return nulls
				return new Object[loadSdf(f)];

				//				throw new IllegalStateException("should not happen, was set on read sdf\n" + f + "\n" + p + "\n"
				//						+ normalize + "\n" + index + "\n->  " + CollectionUtil.toString(values.keySet()));
			}
			else if (p instanceof CDKProperty)
			{
				CDKProperty cdkProp = (CDKProperty) p;
				IMolecule mols[] = sdfToMolecules.get(f);
				switch (cdkProp.desc)
				{
					case SMILES:
						SmilesGenerator sg = new SmilesGenerator();
						String smiles[] = new String[mols.length];
						for (int i = 0; i < mols.length; i++)
							smiles[i] = sg.createSMILES(mols[i]);
						values.put(valuesKey(f, cdkProp), smiles);
						break;
					default:
						IMolecularDescriptor descriptor = cdkProp.newMolecularDescriptor();
						if (descriptor == null)
							throw new IllegalStateException("Not a CDK molecular descriptor: " + cdkProp.desc);

						List<Double[]> vv = new ArrayList<Double[]>();
						for (int j = 0; j < CDKProperty.numFeatureValues(cdkProp.desc); j++)
							vv.add(new Double[mols.length]);

						for (int i = 0; i < mols.length; i++)
						{
							IDescriptorResult res = descriptor.calculate(mols[i]).getValue();
							if (res instanceof IntegerResult)
								vv.get(0)[i] = (double) ((IntegerResult) res).intValue();
							else if (res instanceof DoubleResult)
								vv.get(0)[i] = ((DoubleResult) res).doubleValue();
							else if (res instanceof DoubleArrayResult)
							{
								if (CDKProperty.numFeatureValues(cdkProp.desc) != ((DoubleArrayResult) res).length())
									throw new IllegalStateException("num feature values wrong for '" + cdkProp + "' : "
											+ CDKProperty.numFeatureValues(cdkProp.desc) + " != "
											+ ((DoubleArrayResult) res).length());
								for (int j = 0; j < CDKProperty.numFeatureValues(cdkProp.desc); j++)
									vv.get(j)[i] = ((DoubleArrayResult) res).get(j);
							}
							else if (res instanceof IntegerArrayResult)
							{
								if (CDKProperty.numFeatureValues(cdkProp.desc) != ((IntegerArrayResult) res).length())
									throw new IllegalStateException("num feature values wrong for '" + cdkProp + "' : "
											+ CDKProperty.numFeatureValues(cdkProp.desc) + " != "
											+ ((IntegerArrayResult) res).length());
								for (int j = 0; j < CDKProperty.numFeatureValues(cdkProp.desc); j++)
									vv.get(j)[i] = (double) ((IntegerArrayResult) res).get(j);
							}
							else
								throw new IllegalStateException("Unknown idescriptor result value for '" + cdkProp
										+ "' : " + res.getClass());
						}

						for (int j = 0; j < CDKProperty.numFeatureValues(cdkProp.desc); j++)
						{
							values.put(valuesKey(f, new CDKProperty(cdkProp.desc, j), false), vv.get(j));
							values.put(valuesKey(f, new CDKProperty(cdkProp.desc, j), true),
									ArrayUtil.normalize(vv.get(j)));
						}
				}
			}
		}

		//		System.out.println(ArrayUtil.toString(values.keySet().toArray()));
		//		for (Object[] o : values.values())
		//		{
		//			System.out.println(ArrayUtil.toString(o));
		//		}

		//		System.out.println("get");
		//		System.out.println(valuesKey(f, p, normalize, index));
		//		//System.out.println(values.get()));
		//		System.out.println(ArrayUtil.toString(values.get(new ValuesKey(f, p, normalize, index).toString())));
		return values.get(valuesKey(f, p, normalize));
	}

	public static void computeSmiles(final String file, final List<SmilesOwner> models)
	{
		if (SDFSmiles != null)
			setSmiles(models, getStringFromSdf(file, SDFSmiles));
		else
		{
			if (values.get(valuesKey(file, CDKProperty.SMILES)) == null)
			{
				for (SmilesOwner m : models)
					m.setSmiles("..loading..");
				Thread th = new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						setSmiles(models, getStringFromSdf(file, CDKProperty.SMILES));
					}
				});
				th.start();
			}
			else
				setSmiles(models, getStringFromSdf(file, CDKProperty.SMILES));
		}
	}

	private static void setSmiles(List<SmilesOwner> models, Object smiles[])
	{
		int count = 0;
		for (SmilesOwner m : models)
			m.setSmiles(smiles[count++] + "");
	}

	public static boolean has3D(String dataset)
	{
		loadSdf(dataset);

		if (sdfHas3D.get(dataset) == null)
		{
			boolean has3D = false;

			IMolecule mols[] = sdfToMolecules.get(dataset);
			for (IMolecule iMolecule : mols)
			{
				for (int i = 0; i < iMolecule.getAtomCount(); i++)
				{
					Point3d p = iMolecule.getAtom(i).getPoint3d();
					if (p != null && p.z != 0)
					{
						has3D = true;
						break;
					}
				}
				if (has3D)
					break;
			}
			sdfHas3D.put(dataset, has3D);
		}
		return sdfHas3D.get(dataset);
	}

	public static void generate3D(String sdfFile, String threeDFilename)
	{
		loadSdf(sdfFile);
		try
		{
			SDFWriter writer = new SDFWriter(new FileOutputStream(threeDFilename));

			IMolecule mols[] = sdfToMolecules.get(sdfFile);
			ModelBuilder3D mb3d = ModelBuilder3D.getInstance(TemplateHandler3D.getInstance(), "mm2");
			for (IMolecule iMolecule : mols)
			{
				IMolecule molecule = iMolecule;
				try
				{
					molecule = mb3d.generate3DCoordinates(molecule, true);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				molecule = (IMolecule) AtomContainerManipulator.removeHydrogens(molecule);
				writer.write(molecule);

				if (Settings.isAborted(Thread.currentThread()))
					return;
			}
			writer.close();
		}
		catch (CDKException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

	}
}
