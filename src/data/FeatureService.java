package data;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.vecmath.Point3d;

import main.Settings;
import main.TaskProvider;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.NoSuchAtomTypeException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IMoleculeSet;
import org.openscience.cdk.io.INChIPlainTextReader;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.MDLReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.SMILESReader;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.modeling.builder3d.ModelBuilder3D;
import org.openscience.cdk.modeling.builder3d.TemplateHandler3D;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

import util.ArrayUtil;
import util.StringUtil;
import util.ValueFileCache;
import dataInterface.MoleculeProperty.Type;

public class FeatureService
{
	private HashMap<DatasetFile, IMolecule[]> fileToMolecules = new HashMap<DatasetFile, IMolecule[]>();
	private HashMap<DatasetFile, Boolean> fileHas3D = new HashMap<DatasetFile, Boolean>();
	private HashMap<DatasetFile, LinkedHashSet<IntegratedProperty>> integratedProperties = new HashMap<DatasetFile, LinkedHashSet<IntegratedProperty>>();
	private HashMap<DatasetFile, IntegratedProperty> integratedSmiles = new HashMap<DatasetFile, IntegratedProperty>();
	private HashMap<DatasetFile, String[]> cdkSmiles = new HashMap<DatasetFile, String[]>();

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
		//		Arrays.sort(p, new ToStringComparator());
		return p;
	}

	public IntegratedProperty getIntegratedClusterProperty(DatasetFile dataset)
	{
		IntegratedProperty cProp = null;
		for (IntegratedProperty integratedProperty : integratedProperties.get(dataset))
			if (integratedProperty.toString().toLowerCase().equals("cluster"))
			{
				cProp = integratedProperty;
				break;
			}
		if (cProp == null)
			for (IntegratedProperty integratedProperty : integratedProperties.get(dataset))
				if (integratedProperty.toString().toLowerCase().equals("clusters"))
				{
					cProp = integratedProperty;
					break;
				}
		if (cProp == null)
			for (IntegratedProperty integratedProperty : integratedProperties.get(dataset))
				if (integratedProperty.toString().matches("(?i).*cluster.*"))
				{
					cProp = integratedProperty;
					break;
				}
		return cProp;
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
			String sep = ",";
			boolean smiles = false;
			boolean inchi = false;
			while ((ss = b.readLine()) != null)
			{
				if (firstLine)
				{
					if (StringUtil.numOccurences(ss, ",") < StringUtil.numOccurences(ss, ";"))
						sep = ";";

					int i = 0;
					for (String sss : ss.split(sep + "(?=([^\"]*\"[^\"]*\")*[^\"]*$)"))
					{
						sss = StringUtil.trimQuotes(sss);
						if (i == 0)
						{
							if (sss.matches(".*(?i)smiles.*"))
								smiles = true;
							else if (sss.matches(".*(?i)inchi.*"))
								inchi = true;
							else
								throw new IllegalArgumentException(
										"first argument in csv must be 'smiles' or 'inchi' (is: " + sss + ")");
						}
						propNames.add(sss);
						props.put(sss, new ArrayList<String>());
						i++;
					}
					firstLine = false;
				}
				else if (!ss.startsWith("#") && ss.trim().length() > 0)
				{
					int i = 0;
					for (String sss : ss.split(sep + "(?=([^\"]*\"[^\"]*\")*[^\"]*$)"))
					{
						sss = StringUtil.trimQuotes(sss);
						if (i == 0)
							s.append(sss + " ");
						if (sss.length() == 0)
							sss = null;
						props.get(propNames.get(i)).add(sss);
						i++;
					}
					while (i < props.size())
					{
						props.get(propNames.get(i)).add(null);
						i++;
					}
					s.append("\n");
				}
			}
			List<IAtomContainer> list;
			if (smiles)
			{
				SMILESReader reader = new SMILESReader(new ByteArrayInputStream(s.toString().getBytes()));
				IChemFile content = (IChemFile) reader.read((IChemObject) new ChemFile());
				list = ChemFileManipulator.getAllAtomContainers(content);
				reader.close();
			}
			else if (inchi)
			{
				list = new ArrayList<IAtomContainer>();
				for (String inch : s.toString().split("\n"))
				{
					INChIPlainTextReader reader = new INChIPlainTextReader(new ByteArrayInputStream(inch.getBytes()));
					IChemFile content = (IChemFile) reader.read((IChemObject) new ChemFile());
					List<IAtomContainer> l = ChemFileManipulator.getAllAtomContainers(content);
					if (l.size() != 1)
						throw new Error();
					list.add(l.get(0));
					reader.close();
				}
			}
			else
				throw new Error("Could not read csv file");

			int molCount = 0;
			for (IAtomContainer mol : list)
			{
				for (String p : propNames)
				{
					if (props.get(p).size() != list.size())
						throw new IllegalStateException("num molecules: " + list.size() + ", num values for '" + p
								+ "': " + props.get(p).size());
					mol.setProperty(p, props.get(p).get(molCount));
				}
				molCount++;
			}
			boolean removeSMIdbName = true;
			for (IAtomContainer mol : list)
				if (mol.getProperties().containsKey("SMIdbNAME") && mol.getProperty("SMIdbNAME") != null
						&& mol.getProperty("SMIdbNAME").toString().trim().length() > 0)
					removeSMIdbName = false;
			if (removeSMIdbName)
				for (IAtomContainer mol : list)
					if (mol.getProperties().containsKey("SMIdbNAME"))
						mol.removeProperty("SMIdbNAME");
			return list;
		}
		catch (Exception e)
		{
			if (throwError)
			{
				Settings.LOGGER.error(e);
				throw e;
			}
			return null;
		}
	}

	public static boolean guessNominalFeatureType(int numDistinctFeatures, int datasetSize, boolean numeric)
	{
		if (numeric)
		{
			if (numDistinctFeatures <= 2)
				return true;
		}
		else
		{
			if (numDistinctFeatures <= 5)
				return true;
			if (datasetSize > 200 && numDistinctFeatures <= 10)
				return true;
			if (datasetSize > 1000 && numDistinctFeatures <= 20)
				return true;
		}
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

	public synchronized void updateMoleculesStructure(DatasetFile dataset, boolean threeD)
	{
		if (fileToMolecules.get(dataset) == null)
			throw new IllegalStateException();

		Settings.LOGGER.info("read compounds structures fom file '" + dataset.getSDFPath(threeD) + "' ");

		try
		{
			ISimpleChemObjectReader reader = new ReaderFactory().createReader(new InputStreamReader(
					new FileInputStream(dataset.getSDFPath(threeD))));
			IChemFile content = (IChemFile) reader.read((IChemObject) new ChemFile());
			List<IAtomContainer> list = ChemFileManipulator.getAllAtomContainers(content);
			reader.close();
			Vector<IMolecule> mols = new Vector<IMolecule>();
			for (IAtomContainer iAtomContainer : list)
			{
				IMolecule mol = (IMolecule) iAtomContainer;
				mol = (IMolecule) AtomContainerManipulator.removeHydrogens(mol);
				try
				{
					AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
				}
				catch (NoSuchAtomTypeException e)
				{
					Settings.LOGGER.error(e);
				}
				CDKHueckelAromaticityDetector.detectAromaticity(mol);
				mols.add(mol);
			}
			IMolecule res[] = new IMolecule[mols.size()];
			mols.toArray(res);
			fileToMolecules.put(dataset, res);
		}
		catch (Exception e)
		{
			throw new Error("could not load molecule strcutures " + e, e);
		}
	}

	/**
	 * CDK does not like empty lines between "M END" and the first property
	 */
	public static class SDFReader extends BufferedReader
	{
		public SDFReader(FileInputStream fileInputStream)
		{
			super(new InputStreamReader(fileInputStream));
		}

		private String oldLine = "";

		public String readLine() throws IOException
		{
			String s = super.readLine();
			if (s != null && oldLine != null && oldLine.trim().equals("M  END") && s.trim().length() == 0)
			{
				oldLine = "";
				return readLine();
			}
			else
			{
				oldLine = s;
				return s;
			}
		}
	}

	public synchronized void loadDataset(DatasetFile dataset, boolean loadHydrogen) throws Exception
	{
		if (fileToMolecules.get(dataset) == null)
		{
			Settings.LOGGER.info("read dataset file '" + dataset.getLocalPath() + "' with cdk");
			TaskProvider.verbose("Parsing file with CDK");

			Vector<IMolecule> mols = new Vector<IMolecule>();
			integratedProperties.put(dataset, new LinkedHashSet<IntegratedProperty>());

			int mCount = 0;
			File file = new File(dataset.getLocalPath());
			if (!file.exists())
				throw new IllegalArgumentException("file not found: " + dataset.getLocalPath());

			List<IAtomContainer> list;
			if (dataset.getLocalPath().endsWith(".csv") || dataset.getLocalPath().endsWith("viz"))
			{
				list = readFromCSV(file, true);
				dataset.setFileExtension("csv");
			}
			else if ((list = readFromCSV(file, false)) != null)
			{
				dataset.setFileExtension("csv");
			}
			else
			{
				ISimpleChemObjectReader reader;
				if (dataset.getLocalPath().endsWith(".smi"))
					reader = new SMILESReader(new FileInputStream(file));
				else if (dataset.getLocalPath().endsWith(".sdf"))
					reader = new ReaderFactory().createReader(new SDFReader(new FileInputStream(file)));
				else
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
				for (Object key : mol.getProperties().keySet())
				{
					IntegratedProperty p = IntegratedProperty.create(key.toString(), dataset);
					integratedProperties.get(dataset).add(p);
					if (key.toString().equals("STRUCTURE_SMILES"))
						integratedSmiles.put(dataset, p);
					else if (key.toString().equals("SMILES"))
						integratedSmiles.put(dataset, p);
				}
				if (!TaskProvider.isRunning())
					return;
			}
			for (IAtomContainer iAtomContainer : list)
			{
				IMolecule mol = (IMolecule) iAtomContainer;
				if (!TaskProvider.isRunning())
					return;
				TaskProvider.verbose("Loaded " + (mCount + 1) + "/" + list.size() + " molecules");

				Map<Object, Object> props = mol.getProperties();
				for (IntegratedProperty p : integratedProperties.get(dataset))
				{
					if (!propVals.containsKey(p))
						propVals.put(p, new ArrayList<Object>());
					propVals.get(p).add(props.get(p.getName()));
				}

				mol = (IMolecule) AtomContainerManipulator.removeHydrogens(mol);
				try
				{
					AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
				}
				catch (NoSuchAtomTypeException e)
				{
					Settings.LOGGER.error(e);
				}
				CDKHueckelAromaticityDetector.detectAromaticity(mol);

				// CDKHydrogenAdder ha = CDKHydrogenAdder.getInstance(DefaultChemObjectBuilder.getInstance());
				// try
				// {
				// if (loadHydrogen)
				// {
				// AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
				// ha.addImplicitHydrogens(mol);
				// AtomContainerManipulator.convertImplicitToExplicitHydrogens(mol);
				// }
				//
				// }
				// catch (CDKException e)
				// {
				// Settings.LOGGER.warn("Could not add hydrogens:  " + e.getMessage());
				// }

				if (mol.getAtomCount() == 0)
					illegalMolecules.add(mCount);
				// TaskProvider.task().warning("Could not load molecule (" + mCount + ")", "<not details available>");
				mols.add(mol);
				mCount++;
			}

			if (illegalMolecules.size() > 0)
			{
				Settings.LOGGER.warn("Could not read " + illegalMolecules.size() + "/" + mols.size() + " molecules");
				if (mols.size() > illegalMolecules.size())
					throw new IllegalCompoundsException(illegalMolecules);
				else
					throw new IllegalStateException("Could not read any compounds");
			}
			Settings.LOGGER.info(" done (" + mols.size() + " compounds found)");

			// convert string to double
			for (IntegratedProperty p : integratedProperties.get(dataset))
			{
				List<Object> l = propVals.get(p);
				while (l.size() < mCount)
				{
					// add trailing nulls for missing props
					l.add(null);
				}
				String stringValues[] = new String[l.size()];
				l.toArray(stringValues);

				p.setStringValues(dataset, stringValues);

				Double doubleValues[] = ArrayUtil.parse(stringValues);
				if (doubleValues != null)
				{
					// numericSdfProperties.get(f).add(p);
					p.setTypeAllowed(Type.NOMINAL, true);
					p.setTypeAllowed(Type.NUMERIC, true);
					if (guessNominalFeatureType(p.numDistinctValues(dataset), stringValues.length, true))
						p.setType(Type.NOMINAL);
					else
						p.setType(Type.NUMERIC);
				}
				else
				{
					p.setTypeAllowed(Type.NOMINAL, true);
					p.setTypeAllowed(Type.NUMERIC, false);
					if (guessNominalFeatureType(p.numDistinctValues(dataset), stringValues.length, false))
						p.setType(Type.NOMINAL);
					else
						p.setType(null);
				}
				if (p.getType() == Type.NUMERIC || p.isTypeAllowed(Type.NUMERIC))
					p.setDoubleValues(dataset, doubleValues);
			}

			IMolecule res[] = new IMolecule[mols.size()];
			mols.toArray(res);
			fileToMolecules.put(dataset, res);

			// Settings.LOGGER.println("integrated properties in file: "
			// + CollectionUtil.toString(integratedProperties.get(dataset)));
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
			Settings.LOGGER.error(e);
			return null;
		}
	}

	public String[] getSmiles(DatasetFile dataset)
	{
		if (integratedSmiles.containsKey(dataset))
		{
			String smiles[] = integratedSmiles.get(dataset).getStringValues(dataset);
			SmilesGenerator sg = null;
			for (int i = 0; i < smiles.length; i++)
				if (smiles[i] == null || smiles[i].length() == 0)
				{
					if (sg == null)
						sg = new SmilesGenerator();
					smiles[i] = sg.createSMILES(dataset.getMolecules()[i]);
				}
			return smiles;
		}
		else if (!cdkSmiles.containsKey(dataset))
		{
			String smilesFile = Settings.destinationFile(dataset, dataset.getShortName() + "." + dataset.getMD5()
					+ ".smiles");
			String smiles[];
			if (new File(smilesFile).exists())
			{
				Settings.LOGGER.info("read cached smiles from: " + smilesFile);
				smiles = ValueFileCache.readCacheString(smilesFile, dataset.numCompounds()).get(0);
			}
			else
			{
				Settings.LOGGER.info("compute smiles.. ");
				SmilesGenerator sg = new SmilesGenerator();
				smiles = new String[dataset.getMolecules().length];
				int i = 0;
				for (IMolecule m : dataset.getMolecules())
					smiles[i++] = sg.createSMILES(m);
				Settings.LOGGER.info(" ..done, store: " + smilesFile);
				ValueFileCache.writeCacheString(smilesFile, smiles);
			}
			cdkSmiles.put(dataset, smiles);
		}
		return cdkSmiles.get(dataset);
	}

	public boolean has3D(DatasetFile dataset)
	{
		// loadSdf(dataset);

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
					TaskProvider.warning("Could not build 3D for molecule", e);
				}
				molecule = (IMolecule) AtomContainerManipulator.removeHydrogens(molecule);
				writer.write(molecule);

				if (!TaskProvider.isRunning())
					return;
			}
			writer.close();
		}
		catch (CDKException e)
		{
			Settings.LOGGER.error(e);
		}
		catch (IOException e)
		{
			Settings.LOGGER.error(e);
		}
	}

	/**
	 * does not store properties
	 * 
	 * @param dataset
	 * @param sdfFile
	 * @throws CDKException
	 * @throws IOException
	 */
	public static void writeMoleculesToSDFFile(DatasetFile dataset, String sdfFile) throws CDKException, IOException
	{
		int compoundIndices[] = new int[dataset.numCompounds()];
		for (int i = 0; i < compoundIndices.length; i++)
			compoundIndices[i] = i;
		writeMoleculesToSDFFile(dataset, sdfFile, compoundIndices, false);
	}

	/**
	 * does not store properties
	 * 
	 * @param dataset
	 * @param sdfFile
	 * @param compoundIndices
	 * @param overwrite
	 * @throws CDKException
	 * @throws IOException
	 */
	public static void writeMoleculesToSDFFile(DatasetFile dataset, String sdfFile, int compoundIndices[],
			boolean overwrite) throws CDKException, IOException
	{
		if (dataset.numCompounds() < compoundIndices.length)
			throw new IllegalArgumentException();

		if (!new File(sdfFile).exists() || overwrite)
		{
			File tmpFile = File.createTempFile(dataset.getShortName(), "build.sdf");

			SDFWriter writer = new SDFWriter(new FileOutputStream(tmpFile));
			StructureDiagramGenerator sdg = new StructureDiagramGenerator();

			for (int cIndex : compoundIndices)
			{
				IMolecule molecule = dataset.getMolecules()[cIndex];

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
						Settings.LOGGER.error(e);
						newSet.add(AtomContainerManipulator.removeHydrogens(oldSet.getMolecule(i)));
					}
					if (!TaskProvider.isRunning())
						return;
				}
				writer.write(newSet);
				if (!TaskProvider.isRunning())
					return;
			}
			writer.close();

			boolean res = tmpFile.renameTo(new File(sdfFile));
			res |= tmpFile.delete();
			if (!res)
				throw new Error("renaming or delete file error");
			Settings.LOGGER.info("created 2d sdf file: " + sdfFile);
		}
		else
			Settings.LOGGER.info("sdf 2d file already exists: " + sdfFile);
	}

}
