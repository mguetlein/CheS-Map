package data;

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

import javax.vecmath.Point3d;

import main.Settings;
import main.TaskProvider;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.ChemFile;
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
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

import util.ArrayUtil;
import util.FileUtil;
import util.StringUtil;
import util.ToStringComparator;
import dataInterface.MoleculeProperty.Type;

public class FeatureService
{
	private HashMap<DatasetFile, IMolecule[]> fileToMolecules = new HashMap<DatasetFile, IMolecule[]>();
	private HashMap<DatasetFile, Boolean> fileHas3D = new HashMap<DatasetFile, Boolean>();
	private HashMap<DatasetFile, Set<IntegratedProperty>> integratedProperties = new HashMap<DatasetFile, Set<IntegratedProperty>>();
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
		Arrays.sort(p, new ToStringComparator());
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
		Arrays.sort(p, new ToStringComparator());
		return p;
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
						sss = StringUtil.trimQuotes(sss);
						if (i == 0)
						{
							if (!sss.matches("(?i)smiles"))
								throw new IllegalArgumentException("first argument in csv must be smiles (is: " + sss
										+ ")");
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
						sss = StringUtil.trimQuotes(sss);
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

	public static boolean guessNominalFeatureType(int numDistinctFeatures, int datasetSize)
	{
		if (numDistinctFeatures <= 5)
			return true;
		if (datasetSize > 200 && numDistinctFeatures <= 10)
			return true;
		if (datasetSize > 1000 && numDistinctFeatures <= 20)
			return true;
		return false;
	}

	public static class IllegalCompoundsException extends Exception
	{
		public List<Integer> illegalCompounds;

		public IllegalCompoundsException(List<Integer> illegalCompounds)
		{
			this.illegalCompounds = illegalCompounds;
		}
	}

	public synchronized void loadDataset(DatasetFile dataset, boolean loadHydrogen) throws Exception
	{
		if (fileToMolecules.get(dataset) == null)
		{
			System.out.print("read dataset file '" + dataset.getLocalPath() + "' ");

			Vector<IMolecule> mols = new Vector<IMolecule>();
			integratedProperties.put(dataset, new HashSet<IntegratedProperty>());

			int mCount = 0;
			File file = new File(dataset.getLocalPath());
			if (!file.exists())
				throw new IllegalArgumentException("file not found: " + dataset.getLocalPath());

			List<IAtomContainer> list;
			if (dataset.getLocalPath().endsWith(".csv"))
				list = readFromCSV(file, true);
			else if ((list = readFromCSV(file, false)) != null)
			{
				// read from csv was successfull
			}
			else
			{
				ISimpleChemObjectReader reader;
				if (dataset.getLocalPath().endsWith(".smi"))
					reader = new SMILESReader(new FileInputStream(file));
				reader = new ReaderFactory().createReader(new InputStreamReader(new FileInputStream(file)));
				if (reader == null)
					throw new IllegalArgumentException("Could not determine input file type");
				else if (reader instanceof MDLReader || reader instanceof MDLV2000Reader)
					dataset.setSDFPath(dataset.getLocalPath(), false);
				IChemFile content = (IChemFile) reader.read((IChemObject) new ChemFile());
				list = ChemFileManipulator.getAllAtomContainers(content);
				reader.close();
			}

			List<Integer> illegalMolecules = new ArrayList<Integer>();
			HashMap<IntegratedProperty, List<Object>> propVals = new HashMap<IntegratedProperty, List<Object>>();

			for (IAtomContainer iAtomContainer : list)
			{
				IMolecule mol = (IMolecule) iAtomContainer;
				if (TaskProvider.exists())
					TaskProvider.task().verbose("Loaded " + (mCount + 1) + " molecules");

				Map<Object, Object> props = mol.getProperties();
				for (Object key : props.keySet())
				{
					//						if (ArrayUtil.indexOf(allCDKDescriptors, key.toString()) != -1)
					//							throw new IllegalStateException("sdf-property has equal name as cdk-descriptor: "
					//									+ key.toString());
					IntegratedProperty p = IntegratedProperty.create(key.toString());
					// add key to sdfProperties
					integratedProperties.get(dataset).add(p);

					if (key.toString().equals("STRUCTURE_SMILES"))
						integratedSmiles.put(dataset, p);
					else if (key.toString().equals("SMILES"))
						integratedSmiles.put(dataset, p);

					// add value to values
					if (!propVals.containsKey(p))
						propVals.put(p, new ArrayList<Object>());
					propVals.get(p).add(props.get(key));
				}

				mol = (IMolecule) AtomContainerManipulator.removeHydrogens(mol);

				//			CDKHydrogenAdder ha = CDKHydrogenAdder.getInstance(DefaultChemObjectBuilder.getInstance());
				//				try
				//				{
				//					if (loadHydrogen)
				//					{
				//						AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
				//						ha.addImplicitHydrogens(mol);
				//						AtomContainerManipulator.convertImplicitToExplicitHydrogens(mol);
				//					}
				//
				//				}
				//				catch (CDKException e)
				//				{
				//					System.err.println("Could not add hydrogens:  " + e.getMessage());
				//				}

				if (mol.getAtomCount() == 0)
					illegalMolecules.add(mCount);
				//TaskProvider.task().warning("Could not load molecule (" + mCount + ")", "<not details available>");
				mols.add(mol);
				mCount++;
			}
			if (illegalMolecules.size() > 0)
				if (mols.size() > illegalMolecules.size())
					throw new IllegalCompoundsException(illegalMolecules);
				else
					throw new IllegalStateException("Could not read any compounds");

			System.out.println(" done (" + mols.size() + " compounds found)");

			// convert string to double
			for (IntegratedProperty p : integratedProperties.get(dataset))
			{
				List<Object> l = propVals.get(p);
				while (l.size() < mCount)
				{
					// add trailing nulls for missing props
					l.add(null);
				}
				String o[] = new String[l.size()];
				l.toArray(o);

				Set<Object> distinctValues = ArrayUtil.getDistinctValues(o);
				int numDistinct = distinctValues.size();
				p.setNominalDomain(distinctValues.toArray());

				Double d[] = ArrayUtil.parse(o);
				if (d != null)
				{
					//					numericSdfProperties.get(f).add(p);
					p.setTypeAllowed(Type.NOMINAL, true);
					p.setTypeAllowed(Type.NUMERIC, true);
					if (guessNominalFeatureType(numDistinct, o.length))
						p.setType(Type.NOMINAL);
					else
						p.setType(Type.NUMERIC);
				}
				else
				{
					p.setTypeAllowed(Type.NOMINAL, true);
					p.setTypeAllowed(Type.NUMERIC, false);
					if (guessNominalFeatureType(numDistinct, o.length))
						p.setType(Type.NOMINAL);
					else
						p.setType(null);
				}
				if (p.getType() == Type.NUMERIC)
					p.setDoubleValues(dataset, d);
				else
					p.setStringValues(dataset, o);
			}

			IMolecule res[] = new IMolecule[mols.size()];
			mols.toArray(res);
			fileToMolecules.put(dataset, res);

			//			System.out.println("integrated properties in file: "
			//					+ CollectionUtil.toString(integratedProperties.get(dataset)));
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
			loadDataset(dataset, loadHydrogen);
			return fileToMolecules.get(dataset);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public String[] getSmiles(DatasetFile dataset)
	{
		if (integratedSmiles.containsKey(dataset))
			return integratedSmiles.get(dataset).getStringValues(dataset);
		else
		{
			if (!CDKProperty.SMILES.isValuesSet(dataset))
				CDKProperty.SMILES.getMoleculePropertySet().compute(dataset);
			return CDKProperty.SMILES.getStringValues(dataset);
		}
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
					TaskProvider.task().warning("Could not build 3D for molecule", e);
				}
				molecule = (IMolecule) AtomContainerManipulator.removeHydrogens(molecule);
				writer.write(molecule);

				if (TaskProvider.task().isCancelled())
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
						if (TaskProvider.task().isCancelled())
							return;
					}

					writer.write(newSet);
					if (TaskProvider.task().isCancelled())
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
