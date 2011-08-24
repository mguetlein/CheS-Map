package data;

import gui.Progressable;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
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

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IMoleculeSet;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.MDLReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.SMILESReader;
import org.openscience.cdk.layout.StructureDiagramGenerator;
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
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

import util.ArrayUtil;
import util.CollectionUtil;
import util.FileUtil;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import dataInterface.MoleculePropertySet;

public class FeatureService
{
	private HashMap<DatasetFile, IMolecule[]> fileToMolecules = new HashMap<DatasetFile, IMolecule[]>();
	private HashMap<DatasetFile, Boolean> fileHas3D = new HashMap<DatasetFile, Boolean>();
	private HashMap<DatasetFile, Set<IntegratedProperty>> integratedProperties = new HashMap<DatasetFile, Set<IntegratedProperty>>();
	private HashMap<String, Object[]> values = new HashMap<String, Object[]>();
	private HashMap<DatasetFile, IntegratedProperty> integratedSmiles = new HashMap<DatasetFile, IntegratedProperty>();

	public FeatureService()
	{
	}

	public IntegratedProperty[] getIntegratedProperties(DatasetFile dataset, boolean includingSmiles)
	{
		int offset = (!includingSmiles && integratedSmiles.containsKey(dataset)) ? -1 : 0;
		IntegratedProperty p[] = new IntegratedProperty[integratedProperties.get(dataset).size() + offset];
		int i = 0;
		for (IntegratedProperty pp : integratedProperties.get(dataset))
			if (includingSmiles || !pp.equals(integratedSmiles.get(dataset)))
				p[i++] = pp;
		return p;
	}

	public IntegratedProperty[] getIntegratedClusterProperties(DatasetFile dataset)
	{
		List<IntegratedProperty> props = new ArrayList<IntegratedProperty>();
		for (IntegratedProperty integratedProperty : integratedProperties.get(dataset))
			if (integratedProperty.toString().matches("Prediction feature for cluster assignment.*"))
				props.add(integratedProperty);
		IntegratedProperty p[] = new IntegratedProperty[props.size()];
		props.toArray(p);
		return p;
	}

	private String valuesKey(DatasetFile dataset, MoleculeProperty property)
	{
		return valuesKey(dataset, property, false);
	}

	private String valuesKey(DatasetFile dataset, MoleculeProperty property, boolean normalized)
	{
		return dataset.toString() + "_" + property.toString() + "_" + normalized + "_"
				+ property.getClass().getSimpleName();
	}

	public boolean isLoaded(DatasetFile dataset)
	{
		return (fileToMolecules.get(dataset) != null);
	}

	public void clear(DatasetFile dataset)
	{
		if (fileToMolecules.get(dataset) != null)
		{
			fileToMolecules.remove(dataset);
			fileHas3D.remove(dataset);
			integratedProperties.remove(dataset);
			List<String> toDel = new ArrayList<String>();
			for (String k : values.keySet())
				if (k.startsWith(dataset.toString()))
					toDel.add(k);
			for (String k : toDel)
				values.remove(k);
		}
	}

	private List<IAtomContainer> readFromCSV(File f, boolean throwError) throws Exception
	{
		try
		{
			StringBuffer s = new StringBuffer();
			BufferedReader b = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
			String ss = "";
			boolean firstLine = true;
			List<String> propNames = new ArrayList<String>();
			HashMap<String, List<String>> props = new HashMap<String, List<String>>();
			while ((ss = b.readLine()) != null)
			{
				if (firstLine)
				{
					int i = 0;
					for (String sss : ss.split(","))
					{
						if (i == 0)
						{
							if (!sss.matches("(?i)smiles"))
								throw new IllegalArgumentException("first argument in csv must be smiles");
						}
						propNames.add(sss);
						props.put(sss, new ArrayList<String>());
						i++;
					}
					firstLine = false;
				}
				else
				{
					int i = 0;
					for (String sss : ss.split(","))
					{
						if (i == 0)
							s.append(sss + " ");
						props.get(propNames.get(i)).add(sss);
						i++;
					}
					s.append("\n");
				}
			}
			SMILESReader reader = new SMILESReader(new ByteArrayInputStream(s.toString().getBytes()));
			IChemFile content = (IChemFile) reader.read((IChemObject) new ChemFile());
			List<IAtomContainer> list = ChemFileManipulator.getAllAtomContainers(content);
			reader.close();
			int molCount = 0;
			for (IAtomContainer mol : list)
			{
				for (String p : propNames)
					mol.setProperty(p, props.get(p).get(molCount));
				molCount++;
			}
			return list;
		}
		catch (Exception e)
		{
			if (throwError)
			{
				e.printStackTrace();
				throw e;
			}
			return null;
		}
	}

	public synchronized void loadDataset(DatasetFile dataset, boolean loadHydrogen, final Progressable progress)
			throws Exception
	{
		if (fileToMolecules.get(dataset) == null)
		{
			System.out.print("read dataset file '" + dataset.getLocalPath() + "' ");

			Vector<IMolecule> mols = new Vector<IMolecule>();
			integratedProperties.put(dataset, new HashSet<IntegratedProperty>());

			int mCount = 0;
			File ff = new File(dataset.getLocalPath());
			if (!ff.exists())
				throw new IllegalArgumentException("file not found: " + dataset.getLocalPath());

			CDKHydrogenAdder ha = CDKHydrogenAdder.getInstance(DefaultChemObjectBuilder.getInstance());

			List<IAtomContainer> list;
			if (dataset.getLocalPath().endsWith(".csv"))
				list = readFromCSV(ff, true);
			else if ((list = readFromCSV(ff, false)) != null)
			{
				// read from csv was successfull
			}
			else
			{
				ISimpleChemObjectReader reader;
				if (dataset.getLocalPath().endsWith(".smi"))
					reader = new SMILESReader(new FileInputStream(ff));
				reader = new ReaderFactory().createReader(new InputStreamReader(new FileInputStream(ff)));
				if (reader == null)
					throw new IllegalArgumentException("Could not determine input file type");
				else if (reader instanceof MDLReader || reader instanceof MDLV2000Reader)
					dataset.setSDFPath(dataset.getLocalPath(), false);
				IChemFile content = (IChemFile) reader.read((IChemObject) new ChemFile());
				list = ChemFileManipulator.getAllAtomContainers(content);
				reader.close();
			}

			for (IAtomContainer iAtomContainer : list)
			{
				System.out.print(".");
				IMolecule mol = (IMolecule) iAtomContainer;//reader.next();
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
					IntegratedProperty p = IntegratedProperty.fromString(key.toString());
					// add key to sdfProperties
					integratedProperties.get(dataset).add(p);

					if (key.toString().equals("STRUCTURE_SMILES"))
						integratedSmiles.put(dataset, p);
					else if (key.toString().equals("SMILES"))
						integratedSmiles.put(dataset, p);

					// add value to values
					String valuesKey = valuesKey(dataset, p);
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

			System.out.println(" done (" + mols.size() + " molecules found)");

			// convert string to double
			for (IntegratedProperty p : integratedProperties.get(dataset))
			{
				Object o[] = values.get(valuesKey(dataset, p));
				if (o.length < mCount)
				{
					// add trailing nulls for missing props
					o = Arrays.copyOf(o, mCount);
					values.put(valuesKey(dataset, p), o);
				}
				Set<Object> distinctValues = ArrayUtil.getDistinctValues(o);
				int numDistinct = distinctValues.size();
				p.setNominalDomain(distinctValues.toArray());

				Double d[] = ArrayUtil.parse(o);
				if (d != null)
				{
					//					numericSdfProperties.get(f).add(p);
					p.setTypeAllowed(Type.NOMINAL, true);
					p.setTypeAllowed(Type.NUMERIC, true);
					if (numDistinct <= 5 || numDistinct <= o.length / 20)
						p.setType(Type.NOMINAL);
					else
						p.setType(Type.NUMERIC);

					values.put(valuesKey(dataset, p), o);
					values.put(valuesKey(dataset, p, true), ArrayUtil.normalize(d));
				}
				else
				{
					p.setTypeAllowed(Type.NOMINAL, true);
					p.setTypeAllowed(Type.NUMERIC, false);
					if (numDistinct <= 5 || numDistinct <= o.length / 20)
						p.setType(Type.NOMINAL);
					else
						p.setType(null);

					// normalization of string elements
					values.put(valuesKey(dataset, p, true), ArrayUtil.normalize(o));
				}
			}

			IMolecule res[] = new IMolecule[mols.size()];
			mols.toArray(res);
			fileToMolecules.put(dataset, res);

			System.out.println("integrated properties in file: "
					+ CollectionUtil.toString(integratedProperties.get(dataset)));
		}
	}

	public int numCompounds(DatasetFile dataset)
	{
		return fileToMolecules.get(dataset).length;
	}

	public IMolecule[] getMolecules(DatasetFile dataset)
	{
		return getMolecules(dataset, true);
	}

	public IMolecule[] getMolecules(DatasetFile dataset, boolean loadHydrogen)
	{
		try
		{
			loadDataset(dataset, loadHydrogen, null);
			return fileToMolecules.get(dataset);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public boolean isComputed(DatasetFile dataset, MoleculePropertySet prop)
	{
		return (values.get(valuesKey(dataset, prop.get(0), false)) != null);
	}

	public Object[] getValues(DatasetFile dataset, MoleculeProperty p, boolean normalize)
	{
		if (values.get(valuesKey(dataset, p, normalize)) == null)
		{
			if (p instanceof IntegratedProperty)
			{
				// this can happen if a cluster has only null values for a integrated property-> return nulls
				return new Object[fileToMolecules.get(dataset).length];
			}
			if (p instanceof StructuralAlerts.Alert)
			{
				StructuralAlerts.Alert alert = (StructuralAlerts.Alert) p;

				String match[] = Settings.SMARTS_HANDLER.match(alert, dataset);
				values.put(valuesKey(dataset, alert, false), match);
				values.put(valuesKey(dataset, alert, true), ArrayUtil.normalize(match));
			}
			if (p instanceof OBFingerprintProperty)
			{
				OBFingerprintProperty obProp = (OBFingerprintProperty) p;

				List<String> fingerprintsForMolecules = obProp.compute(dataset);
				if (fingerprintsForMolecules.size() != fileToMolecules.get(dataset).length)
					throw new IllegalStateException("num molecules not correct");
				if (fingerprintsForMolecules.get(0).length() != obProp.numSetValues())
					throw new IllegalStateException("fingerprint length not correct");

				List<String[]> featureValues = new ArrayList<String[]>();
				for (int j = 0; j < fingerprintsForMolecules.get(0).length(); j++)
				{
					String[] featureValue = new String[fingerprintsForMolecules.size()];
					for (int i = 0; i < featureValue.length; i++)
						featureValue[i] = fingerprintsForMolecules.get(i).charAt(j) + "";
					featureValues.add(featureValue);
				}

				for (int j = 0; j < obProp.numSetValues(); j++)
				{
					values.put(valuesKey(dataset, OBFingerprintProperty.create(obProp.type, j), false),
							featureValues.get(j));
					values.put(valuesKey(dataset, OBFingerprintProperty.create(obProp.type, j), true),
							ArrayUtil.normalize(featureValues.get(j)));
				}
			}
			else if (p instanceof CDKProperty)
			{
				CDKProperty cdkProp = (CDKProperty) p;
				IMolecule mols[] = fileToMolecules.get(dataset);
				if (cdkProp == CDKProperty.SMILES)
				{
					SmilesGenerator sg = new SmilesGenerator();
					String smiles[] = new String[mols.length];
					for (int i = 0; i < mols.length; i++)
						smiles[i] = sg.createSMILES(mols[i]);
					values.put(valuesKey(dataset, cdkProp), smiles);
				}
				else
				{
					IMolecularDescriptor descriptor = cdkProp.newMolecularDescriptor();
					if (descriptor == null)
						throw new IllegalStateException("Not a CDK molecular descriptor: " + cdkProp.desc);

					List<Double[]> vv = new ArrayList<Double[]>();
					for (int j = 0; j < cdkProp.numSetValues(); j++)
						vv.add(new Double[mols.length]);

					for (int i = 0; i < mols.length; i++)
					{
						try
						{
							IDescriptorResult res = descriptor.calculate(mols[i]).getValue();
							if (res instanceof IntegerResult)
								vv.get(0)[i] = (double) ((IntegerResult) res).intValue();
							else if (res instanceof DoubleResult)
								vv.get(0)[i] = ((DoubleResult) res).doubleValue();
							else if (res instanceof DoubleArrayResult)
							{
								if (cdkProp.numSetValues() != ((DoubleArrayResult) res).length())
									throw new IllegalStateException("num feature values wrong for '" + cdkProp + "' : "
											+ cdkProp.numSetValues() + " != " + ((DoubleArrayResult) res).length());
								for (int j = 0; j < cdkProp.numSetValues(); j++)
									vv.get(j)[i] = ((DoubleArrayResult) res).get(j);
							}
							else if (res instanceof IntegerArrayResult)
							{
								if (cdkProp.numSetValues() != ((IntegerArrayResult) res).length())
									throw new IllegalStateException("num feature values wrong for '" + cdkProp + "' : "
											+ cdkProp.numSetValues() + " != " + ((IntegerArrayResult) res).length());
								for (int j = 0; j < cdkProp.numSetValues(); j++)
									vv.get(j)[i] = (double) ((IntegerArrayResult) res).get(j);
							}
							else
								throw new IllegalStateException("Unknown idescriptor result value for '" + cdkProp
										+ "' : " + res.getClass());
						}
						catch (Exception e)
						{
							System.err.println("could not compute cdk feature " + e.getMessage());
							e.printStackTrace();

							for (int j = 0; j < cdkProp.numSetValues(); j++)
								vv.get(j)[i] = 0.0;
						}

						if (Settings.isAborted(Thread.currentThread()))
							return null;
					}

					for (int j = 0; j < cdkProp.numSetValues(); j++)
					{
						values.put(valuesKey(dataset, CDKProperty.create(cdkProp.desc, j), false), vv.get(j));
						values.put(valuesKey(dataset, CDKProperty.create(cdkProp.desc, j), true),
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
		return values.get(valuesKey(dataset, p, normalize));
	}

	public String[] getSmiles(DatasetFile dataset)
	{
		if (integratedSmiles.containsKey(dataset))
			return ArrayUtil.cast(String.class, getValues(dataset, integratedSmiles.get(dataset), false));
		else
			return ArrayUtil.cast(String.class, getValues(dataset, CDKProperty.SMILES, false));
	}

	public boolean has3D(DatasetFile dataset)
	{
		//		loadSdf(dataset);

		if (fileHas3D.get(dataset) == null)
		{
			boolean has3D = false;

			IMolecule mols[] = fileToMolecules.get(dataset);
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
			fileHas3D.put(dataset, has3D);
		}
		return fileHas3D.get(dataset);
	}

	public static void generateCDK3D(DatasetFile dataset, String threeDFilename, String forcefield)
	{
		try
		{
			SDFWriter writer = new SDFWriter(new FileOutputStream(threeDFilename));

			IMolecule mols[] = dataset.getMolecules();
			ModelBuilder3D mb3d = ModelBuilder3D.getInstance(TemplateHandler3D.getInstance(), forcefield);
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

	public static void writeSDFFile(DatasetFile dataset)
	{
		try
		{
			String md5 = FileUtil.getMD5String(dataset.getLocalPath());
			File f = new File(dataset.getLocalPath());
			String sdfFile = Settings.destinationFile(f.getParent(), FileUtil.getFilename(f.getAbsolutePath(), false)
					+ md5 + ".sdf");
			if (!new File(sdfFile).exists())
			{
				File tmpFile = File.createTempFile("sdf_build", "tmp");

				SDFWriter writer = new SDFWriter(new FileOutputStream(tmpFile));
				//	ModelBuilder3D mb3d = ModelBuilder3D.getInstance(TemplateHandler3D.getInstance(), "mm2");
				StructureDiagramGenerator sdg = new StructureDiagramGenerator();
				for (IMolecule iMolecule : dataset.getMolecules())
				{
					IMolecule molecule = iMolecule;

					IMoleculeSet oldSet = ConnectivityChecker.partitionIntoMolecules(molecule);
					AtomContainer newSet = new AtomContainer();
					for (int i = 0; i < oldSet.getMoleculeCount(); i++)
					{
						try
						{
							sdg.setMolecule(oldSet.getMolecule(i));
							sdg.generateCoordinates();
							newSet.add(AtomContainerManipulator.removeHydrogens(sdg.getMolecule()));
						}
						catch (Exception e)
						{
							e.printStackTrace();
							newSet.add(AtomContainerManipulator.removeHydrogens(oldSet.getMolecule(i)));
						}
						if (Settings.isAborted(Thread.currentThread()))
							return;
					}

					writer.write(newSet);
					if (Settings.isAborted(Thread.currentThread()))
						return;
				}
				writer.close();

				boolean res = tmpFile.renameTo(new File(sdfFile));
				res |= tmpFile.delete();
				if (!res)
					throw new Error("renaming or delete file error");
				System.out.println("created 2d sdf file: " + sdfFile);
			}
			else
				System.out.println("sdf 2d file already exists: " + sdfFile);
			dataset.setSDFPath(sdfFile, false);
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
