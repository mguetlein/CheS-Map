package org.chesmapper.map.data;

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

import org.chesmapper.map.dataInterface.CompoundProperty;
import org.chesmapper.map.dataInterface.NominalProperty;
import org.chesmapper.map.dataInterface.CompoundPropertySet.Type;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.main.TaskProvider;
import org.chesmapper.map.property.IntegratedPropertySet;
import org.chesmapper.map.util.ValueFileCache;
import org.mg.javalib.io.SDFUtil;
import org.mg.javalib.util.ArrayUtil;
import org.mg.javalib.util.FileUtil;
import org.mg.javalib.util.ListUtil;
import org.mg.javalib.util.FileUtil.UnexpectedNumColsException;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.NoSuchAtomTypeException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemObject;
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
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.FixBondOrdersTool;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

public class FeatureService
{
	private HashMap<DatasetFile, IAtomContainer[]> fileToCompounds = new HashMap<DatasetFile, IAtomContainer[]>();
	private HashMap<DatasetFile, Boolean> fileHas3D = new HashMap<DatasetFile, Boolean>();
	private HashMap<DatasetFile, LinkedHashSet<IntegratedPropertySet>> integratedProperties = new HashMap<DatasetFile, LinkedHashSet<IntegratedPropertySet>>();
	private HashMap<DatasetFile, IntegratedPropertySet> integratedSmiles = new HashMap<DatasetFile, IntegratedPropertySet>();
	private HashMap<DatasetFile, String[]> cdkSmiles = new HashMap<DatasetFile, String[]>();

	public FeatureService()
	{
	}

	public IntegratedPropertySet[] getIntegratedProperties(DatasetFile dataset)
	{
		return ArrayUtil.toArray(IntegratedPropertySet.class, integratedProperties.get(dataset));
	}

	public IntegratedPropertySet getIntegratedClusterProperty(DatasetFile dataset)
	{
		IntegratedPropertySet cProp = null;
		for (IntegratedPropertySet integratedProperty : integratedProperties.get(dataset))
			if (integratedProperty.toString().toLowerCase().equals("cluster"))
			{
				cProp = integratedProperty;
				break;
			}
		if (cProp == null)
			for (IntegratedPropertySet integratedProperty : integratedProperties.get(dataset))
				if (integratedProperty.toString().toLowerCase().equals("clusters"))
				{
					cProp = integratedProperty;
					break;
				}
		if (cProp == null)
			for (IntegratedPropertySet integratedProperty : integratedProperties.get(dataset))
				if (integratedProperty.toString().matches("(?i).*cluster.*"))
				{
					cProp = integratedProperty;
					break;
				}
		return cProp;
	}

	public synchronized boolean isLoaded(DatasetFile dataset)
	{
		return (fileToCompounds.get(dataset) != null);
	}

	public synchronized void clear(DatasetFile dataset)
	{
		if (fileToCompounds.get(dataset) != null)
		{
			fileToCompounds.remove(dataset);
			fileHas3D.remove(dataset);
			integratedProperties.remove(dataset);
			integratedSmiles.remove(dataset);
			cdkSmiles.remove(dataset);
		}
	}

	//	public static boolean testSmilesProp() throws Exception
	//	{
	//		MySmilesParser smilesParser = new MySmilesParser();
	//		IAtomContainer mol = smilesParser.parseSmiles("C");
	//		if (!ObjectUtil.equals(mol.getProperties().get("SMILES"), "C"))
	//			throw new IllegalStateException("Smiles property not set");
	//		return true;
	//	}

	private List<IAtomContainer> readFromCSV(File f, boolean throwError) throws Exception
	{
		try
		{
			StringBuffer smilesInchiContent = new StringBuffer();
			List<String> propNames = new ArrayList<String>();
			HashMap<String, List<String>> props = new HashMap<String, List<String>>();
			boolean smiles = false;
			boolean inchi = false;
			FileUtil.CSVFile csvFile = FileUtil.readCSV(f.getAbsolutePath());
			int rowIndex = 0;
			List<Integer> emptySmiles = new ArrayList<Integer>();
			for (String line[] : csvFile.content)
			{
				if (rowIndex == 0)
				{
					int columnIndex = 0;
					for (String value : line)
					{
						if (columnIndex == 0)
						{
							if (value.matches(".*(?i)smiles.*"))
								smiles = true;
							else if (value.matches(".*(?i)inchi.*"))
								inchi = true;
							else
								throw new IllegalArgumentException(
										"first argument in csv must be 'smiles' or 'inchi' (is: " + value + ")");
						}
						propNames.add(value);
						props.put(value, new ArrayList<String>());
						columnIndex++;
					}
				}
				else
				{
					int columnIndex = 0;
					for (String value : line)
					{
						if (columnIndex == 0)
						{
							if (value == null)
							{
								emptySmiles.add(rowIndex + 1);
								if (smiles)
									value = "C12CC2C1";
								else
									value = "InChI=1S/C4H6/c1-3-2-4(1)3/h3-4H,1-2H2";
							}
							smilesInchiContent.append(value + " ");
						}
						props.get(propNames.get(columnIndex)).add(value);
						columnIndex++;
					}
					while (columnIndex < props.size())
					{
						props.get(propNames.get(columnIndex)).add(null);
						columnIndex++;
					}
					smilesInchiContent.append("\n");
				}
				rowIndex++;
			}
			if (emptySmiles.size() > 0)
				Settings.LOGGER.warn("Empty " + (smiles ? "smiles" : "inchi") + " in row/s "
						+ ListUtil.toString(emptySmiles));

			List<IAtomContainer> list;
			if (smiles)
			{
				SMILESReader reader = new SMILESReader(new ByteArrayInputStream(smilesInchiContent.toString()
						.getBytes()));
				IChemFile content = (IChemFile) reader.read((IChemObject) new ChemFile());
				reader.close();
				list = ChemFileManipulator.getAllAtomContainers(content);
			}
			else if (inchi)
			{
				list = new ArrayList<IAtomContainer>();
				for (String inch : smilesInchiContent.toString().split("\n"))
				{
					INChIPlainTextReader reader = new INChIPlainTextReader(new ByteArrayInputStream(inch.getBytes()));
					IChemFile content = (IChemFile) reader.read((IChemObject) new ChemFile());
					reader.close();
					List<IAtomContainer> l = ChemFileManipulator.getAllAtomContainers(content);
					if (l.size() != 1)
						throw new Error("Could not read inchi: " + inch);
					list.add(l.get(0));
				}
			}
			else
				throw new Error("Could not read csv file");
			if (list.size() != (csvFile.content.size() - 1) && smiles)
			{
				System.err.println("wrong num molecules checking smiles");
				rowIndex = 0;
				SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
				for (String smilesString : smilesInchiContent.toString().split("\n"))
				{
					smilesString = smilesString.trim();
					IAtomContainer molecule = null;
					String error = "";
					try
					{
						molecule = smilesParser.parseSmiles(smilesString);
					}
					catch (Exception ex)
					{
						error = ", error: " + ex.getMessage();
					}
					if (molecule == null || molecule.getAtomCount() == 0)
						throw new IllegalArgumentException("Illegal smiles '" + smilesString + "' in row "
								+ (rowIndex + 1) + error);
					rowIndex++;
				}
			}

			int molCount = 0;
			for (IAtomContainer mol : list)
			{
				int pCount = 0;
				for (String p : propNames)
				{
					if (props.get(p).size() != list.size())
						throw new IllegalStateException("num molecules: " + list.size() + ", num values for '" + p
								+ "': " + props.get(p).size());
					String prop = pCount == 0 ? (smiles ? "SMILES" : "InChI") : p;
					mol.setProperty(prop, props.get(p).get(molCount));
					pCount++;
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

	public synchronized void updateCompoundStructureFrom2DSDF(DatasetFile dataset)
	{
		updateCompoundStructure(dataset, dataset.getSDF());
	}

	public synchronized void updateCompoundStructureFrom3DSDF(DatasetFile dataset)
	{
		updateCompoundStructure(dataset, dataset.getSDF3D());
	}

	private synchronized void updateCompoundStructure(DatasetFile dataset, String sdf)
	{
		if (fileToCompounds.get(dataset) == null)
			throw new IllegalStateException();

		Settings.LOGGER.info("read compounds structures fom file '" + sdf + "' ");

		try
		{
			ISimpleChemObjectReader reader = new ReaderFactory().createReader(new InputStreamReader(
					new FileInputStream(sdf)));
			IChemFile content = (IChemFile) reader.read((IChemObject) new ChemFile());
			List<IAtomContainer> list = ChemFileManipulator.getAllAtomContainers(content);
			reader.close();
			Vector<IAtomContainer> mols = new Vector<IAtomContainer>();

			// mimics the old CDKHuckelAromaticityDetector which uses the CDK atom types
			ElectronDonation model = ElectronDonation.cdk();
			CycleFinder cycles = Cycles.cdkAromaticSet();
			Aromaticity aromaticity = new Aromaticity(model, cycles);

			for (IAtomContainer iAtomContainer : list)
			{
				IAtomContainer mol = (IAtomContainer) iAtomContainer;
				mol = (IAtomContainer) AtomContainerManipulator.removeHydrogens(mol);
				try
				{
					AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
				}
				catch (NoSuchAtomTypeException e)
				{
					Settings.LOGGER.error(e);
				}
				aromaticity.apply(mol);
				mols.add(mol);
			}
			IAtomContainer res[] = new IAtomContainer[mols.size()];
			mols.toArray(res);
			fileToCompounds.put(dataset, res);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Error("could not load molecule structures " + e, e);
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

	public synchronized void loadDataset(DatasetFile dataset) throws Exception
	{
		if (fileToCompounds.get(dataset) == null)
		{
			//Settings.LOGGER.info("read dataset file '" + dataset.getLocalPath() + "' with cdk");
			TaskProvider.debug("Parsing file with CDK");

			Vector<IAtomContainer> mols = new Vector<IAtomContainer>();
			integratedProperties.put(dataset, new LinkedHashSet<IntegratedPropertySet>());

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
			//			else if ((list = readFromCSV(file, false)) != null)
			//			{
			//				dataset.setFileExtension("csv");
			//			}
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
					dataset.setSDF(dataset.getLocalPath());
				IChemFile content = (IChemFile) reader.read((IChemObject) new ChemFile());
				list = ChemFileManipulator.getAllAtomContainers(content);
				// do that manually to not overwrite smiles parser
				if (dataset.getLocalPath().endsWith(".smi"))
				{
					String smilesFileContent[] = FileUtil.readStringFromFile(dataset.getLocalPath()).split("\n");
					if (smilesFileContent[smilesFileContent.length - 1].trim().length() == 0)
						smilesFileContent = ArrayUtil.removeAt(String.class, smilesFileContent,
								smilesFileContent.length - 1);
					if (list.size() != smilesFileContent.length)
						throw new IllegalStateException("num compounds in smiles file does not fit, lines: "
								+ smilesFileContent.length + " != num-compounds-parsed: " + list.size());
					for (int i = 0; i < smilesFileContent.length; i++)
						list.get(i).setProperty("SMILES", smilesFileContent[i].split("[\\s\\t]+")[0]);
				}
				reader.close();
			}

			List<Integer> illegalCompounds = new ArrayList<Integer>();
			HashMap<IntegratedPropertySet, List<Object>> propVals = new HashMap<IntegratedPropertySet, List<Object>>();

			for (IAtomContainer iAtomContainer : list)
			{
				IAtomContainer mol = (IAtomContainer) iAtomContainer;
				for (Object key : mol.getProperties().keySet())
				{
					if (key == null)
						throw new Error("null key in dataset, empty column header?");
					IntegratedPropertySet p = IntegratedPropertySet.create(key.toString(), dataset);
					integratedProperties.get(dataset).add(p);
					if (key.toString().toUpperCase().equals("STRUCTURE_SMILES")
							|| key.toString().toUpperCase().equals("SMILES"))
					{
						integratedSmiles.put(dataset, p);
						p.setSmiles(true);
					}
				}
				if (!TaskProvider.isRunning())
					return;
			}
			// mimics the old CDKHuckelAromaticityDetector which uses the CDK atom types
			ElectronDonation model = ElectronDonation.cdk();
			CycleFinder cycles = Cycles.cdkAromaticSet();
			Aromaticity aromaticity = new Aromaticity(model, cycles);

			for (IAtomContainer iAtomContainer : list)
			{
				IAtomContainer mol = (IAtomContainer) iAtomContainer;
				if (!TaskProvider.isRunning())
					return;
				TaskProvider.verbose("Loaded " + (mCount + 1) + "/" + list.size() + " compounds");

				Map<Object, Object> props = mol.getProperties();
				for (IntegratedPropertySet p : integratedProperties.get(dataset))
				{
					if (!propVals.containsKey(p))
						propVals.put(p, new ArrayList<Object>());
					propVals.get(p).add(props.get(p.get().getName()));
				}

				mol = (IAtomContainer) AtomContainerManipulator.removeHydrogens(mol);
				try
				{
					AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
				}
				catch (NoSuchAtomTypeException e)
				{
					Settings.LOGGER.error(e);
				}
				aromaticity.apply(mol);

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
					illegalCompounds.add(mCount);
				// TaskProvider.task().warning("Could not load compound (" + mCount + ")", "<not details available>");
				mols.add(mol);
				mCount++;
			}

			if (illegalCompounds.size() > 0)
			{
				Settings.LOGGER.warn("Could not read " + illegalCompounds.size() + "/" + mols.size() + " compounds");
				if (dataset.getLocalPath().endsWith(".sdf"))
				{
					String sdf[] = SDFUtil.readSdf(dataset.getLocalPath());
					for (Integer i : illegalCompounds)
						Settings.LOGGER
								.warn("Could not read compund with index '" + i + "':\n>>>\n" + sdf[i] + "\n<<<");
				}
				if (mols.size() > illegalCompounds.size())
					throw new IllegalCompoundsException(illegalCompounds);
				else
					throw new IllegalStateException("Could not read any compounds");
			}
			Settings.LOGGER.info(mols.size() + " compounds found");

			// convert string to double
			for (IntegratedPropertySet p : integratedProperties.get(dataset))
			{
				CompoundProperty prop = p.get();
				List<Object> l = propVals.get(p);
				while (l.size() < mCount)
				{
					// add trailing nulls for missing props
					l.add(null);
				}
				String stringValues[] = new String[l.size()];
				l.toArray(stringValues);

				p.setStringValues(stringValues);

				Double doubleValues[] = ArrayUtil.parse(stringValues);
				if (doubleValues != null)
				{
					// numericSdfProperties.get(f).add(p);
					p.setTypeAllowed(Type.NOMINAL, true);
					p.setTypeAllowed(Type.NUMERIC, true);
					if (guessNominalFeatureType(prop.numDistinctValues(), stringValues.length, true))
						p.setType(Type.NOMINAL);
					else
						p.setType(Type.NUMERIC);
				}
				else
				{
					p.setTypeAllowed(Type.NOMINAL, true);
					p.setTypeAllowed(Type.NUMERIC, false);
					if (guessNominalFeatureType(prop.numDistinctValues(), stringValues.length, false))
						p.setType(Type.NOMINAL);
					else
						p.setType(null);
				}
				if (p.getType() == Type.NUMERIC || p.isTypeAllowed(Type.NUMERIC))
					p.setDoubleValues(doubleValues);
			}

			IAtomContainer res[] = new IAtomContainer[mols.size()];
			mols.toArray(res);
			fileToCompounds.put(dataset, res);

			// Settings.LOGGER.println("integrated properties in file: "
			// + CollectionUtil.toString(integratedProperties.get(dataset)));
		}
	}

	public synchronized int numCompounds(DatasetFile dataset)
	{
		return fileToCompounds.get(dataset).length;
	}

	public synchronized IAtomContainer[] getCompounds(DatasetFile dataset)
	{
		try
		{
			loadDataset(dataset);
			return fileToCompounds.get(dataset);
		}
		catch (Exception e)
		{
			Settings.LOGGER.error(e);
			return null;
		}
	}

	public synchronized String[] getSmiles(DatasetFile dataset)
	{
		if (integratedSmiles.containsKey(dataset))
		{
			String smiles[] = ((NominalProperty) integratedSmiles.get(dataset).get()).getStringValues();
			SmilesGenerator sg = null;
			for (int i = 0; i < smiles.length; i++)
				if (smiles[i] == null || smiles[i].length() == 0)
				{
					if (sg == null)
						sg = new SmilesGenerator();
					try
					{
						smiles[i] = sg.create(dataset.getCompounds()[i]);
					}
					catch (CDKException e)
					{
						TaskProvider.warning("Cannont create smiles for compound " + i, e);
					}
				}
			return smiles;
		}
		else if (!cdkSmiles.containsKey(dataset))
		{
			String smilesFile = Settings.destinationFile(dataset, "smiles");
			String smiles[] = null;
			if (new File(smilesFile).exists())
			{
				Settings.LOGGER.info("Read cached smiles from: " + smilesFile);
				try
				{
					smiles = ValueFileCache.readCacheString(smilesFile, dataset.numCompounds()).get(0);
				}
				catch (UnexpectedNumColsException e)
				{
					Settings.LOGGER.error(e);
				}
			}
			if (smiles == null)
			{
				Settings.LOGGER.info("compute smiles.. ");
				SmilesGenerator sg = new SmilesGenerator();
				smiles = new String[dataset.getCompounds().length];
				int i = 0;
				for (IAtomContainer m : dataset.getCompounds())
				{
					try
					{
						smiles[i] = sg.create(m);
					}
					catch (CDKException e)
					{
						TaskProvider.warning("Cannont create smiles for compound " + i, e);
					}
					i++;
				}
				Settings.LOGGER.info(" ..done, store: " + smilesFile);
				ValueFileCache.writeCacheString(smilesFile, smiles);
			}
			cdkSmiles.put(dataset, smiles);
		}
		return cdkSmiles.get(dataset);
	}

	public synchronized boolean has3D(DatasetFile dataset)
	{
		// loadSdf(dataset);

		if (fileHas3D.get(dataset) == null)
		{
			boolean has3D = false;

			IAtomContainer mols[] = fileToCompounds.get(dataset);
			for (IAtomContainer molecule : mols)
			{
				for (int i = 0; i < molecule.getAtomCount(); i++)
				{
					Point3d p = molecule.getAtom(i).getPoint3d();
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

	public static boolean[] generateCDK3D(DatasetFile dataset, String threeDFilename, String forcefield)
	{
		boolean valid[] = new boolean[dataset.getCompounds().length];
		try
		{
			if (new File(threeDFilename).exists())
				new File(threeDFilename).delete();

			IAtomContainer mols[] = dataset.getCompounds();
			ModelBuilder3D mb3d = ModelBuilder3D.getInstance(TemplateHandler3D.getInstance(), forcefield,
					DefaultChemObjectBuilder.getInstance());

			int count = 0;
			for (IAtomContainer iMolecule : mols)
			{
				IAtomContainer molecule = iMolecule;
				try
				{
					molecule = mb3d.generate3DCoordinates(molecule, true);
					valid[count] = true;
				}
				catch (Exception e)
				{
					TaskProvider.warning("Could not build 3D for compound", e);
				}
				molecule = (IAtomContainer) AtomContainerManipulator.removeHydrogens(molecule);

				SDFWriter writer = new SDFWriter(new FileOutputStream(threeDFilename, true));
				writer.write(molecule);
				writer.close();
				count++;

				if (!TaskProvider.isRunning())
					return null;
			}
		}
		catch (CDKException e)
		{
			Settings.LOGGER.error(e);
		}
		catch (IOException e)
		{
			Settings.LOGGER.error(e);
		}
		return valid;
	}

	/**
	 * does not store properties
	 * 
	 * @param dataset
	 * @param sdfFile
	 * @throws CDKException
	 * @throws IOException
	 */
	public static void writeCompoundsToSDFile(DatasetFile dataset, String sdfFile) throws CDKException, IOException
	{
		int compoundOrigIndices[] = new int[dataset.numCompounds()];
		for (int i = 0; i < compoundOrigIndices.length; i++)
			compoundOrigIndices[i] = i;
		writeOrigCompoundsToSDFile(dataset.getCompounds(), sdfFile, compoundOrigIndices, false, false);
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
	public static void writeOrigCompoundsToSDFile(IAtomContainer molecules[], String sdfFile,
			int compoundOrigIndices[], boolean overwrite, boolean bigData) throws CDKException, IOException
	{
		if (molecules.length < compoundOrigIndices.length)
			throw new IllegalArgumentException();

		if (!new File(sdfFile).exists() || overwrite || !Settings.CACHING_ENABLED)
		{
			File tmpFile = File.createTempFile("tmp-dataset", "build.sdf");
			SDFWriter writer = new SDFWriter(new FileOutputStream(tmpFile));
			try
			{
				StructureDiagramGenerator sdg = new StructureDiagramGenerator();
				FixBondOrdersTool fix = new FixBondOrdersTool();

				for (int cIndex : compoundOrigIndices)
				{
					IAtomContainer molecule = molecules[cIndex];

					IAtomContainerSet oldSet = ConnectivityChecker.partitionIntoMolecules(molecule);
					AtomContainer newSet = new AtomContainer();
					if (bigData)
						newSet.addAtom(new Atom("C"));
					else
					{
						for (int i = 0; i < oldSet.getAtomContainerCount(); i++)
						{
							IAtomContainer mol;
							try
							{
								sdg.setMolecule(oldSet.getAtomContainer(i));
								sdg.generateCoordinates();
								mol = sdg.getMolecule();
							}
							catch (Exception e)
							{
								Settings.LOGGER.error(e);
								mol = oldSet.getAtomContainer(i);
							}
							mol = (IAtomContainer) AtomContainerManipulator.removeHydrogens(mol);
							try
							{
								mol = fix.kekuliseAromaticRings(mol);
							}
							catch (Exception e)
							{
								Settings.LOGGER.error(e);
							}
							newSet.add(mol);

							if (!TaskProvider.isRunning())
								return;
						}
					}
					if (molecule.getProperty("SMIdbNAME") != null) // set identifier in sdf file (title) with identifier in SMI file (SMIdbNAME)
						newSet.setProperty(CDKConstants.TITLE, molecule.getProperty("SMIdbNAME"));
					writer.write(newSet);
					if (!TaskProvider.isRunning())
						return;
				}
			}
			finally
			{
				writer.close();
			}
			if (!FileUtil.robustRenameTo(tmpFile, new File(sdfFile)))
				throw new Error("renaming or delete file error");
			Settings.LOGGER.info("write cdk compounds to sd-file: " + sdfFile);
		}
		else
			Settings.LOGGER.info("cdk sd-file alread exists: " + sdfFile);
	}
}
