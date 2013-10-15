package data.desc;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.Settings;

import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IMoleculeSet;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.smiles.SmilesGenerator;

import util.ArrayUtil;
import data.DatasetFile;
import data.cdk.CDKDescriptor;
import data.obdesc.OBDescriptorFactory;
import dataInterface.CompoundProperty.Type;

public class DescriptorForMixturesHandler
{
	private static boolean DEBUG = true;

	public static Double[] computeCDKDescriptor(CDKDescriptor desc, IMolecule mol)
	{
		if (!isMixture(mol))
			return desc.computeDescriptor(mol);
		else
		{
			IMoleculeSet set = split(mol);
			List<Double[]> results = new ArrayList<Double[]>();
			if (DEBUG)
			{
				String msg = "computing descriptor for mixture from ";
				for (int i = 0; i < set.getAtomContainerCount(); i++)
					msg += sg.createSMILES(set.getAtomContainer(i)) + " ";
				msg += ", instead of from " + sg.createSMILES(mol);
				System.err.println(msg);
			}
			for (int i = 0; i < set.getAtomContainerCount(); i++)
				results.add(desc.computeDescriptor(set.getAtomContainer(i)));
			return ArrayUtil.computeMean(results);
		}
	}

	private static boolean isMixture(IMolecule mol)
	{
		split(mol);
		return mixture.get(mol);
	}

	private static HashMap<IMolecule, IMoleculeSet> split = new HashMap<IMolecule, IMoleculeSet>();
	private static HashMap<IMolecule, Boolean> mixture = new HashMap<IMolecule, Boolean>();
	private static SmilesGenerator sg = new SmilesGenerator();

	private static IMoleculeSet split(IMolecule mol)
	{
		if (!split.containsKey(mol))
		{
			IMoleculeSet set = ConnectivityChecker.partitionIntoMolecules(mol);
			if (set.getAtomContainerCount() > 1)
			{
				mixture.put(mol, true);
				if (DEBUG)
					System.err.println("Mixture found: " + set.getAtomContainerCount() + " : " + sg.createSMILES(mol));
			}
			else
				mixture.put(mol, false);
			while (set.getAtomContainerCount() > 1)
			{
				int minNumAtoms = Integer.MAX_VALUE;
				int maxNumAtoms = Integer.MIN_VALUE;
				int minNumAtomsIndex = -1;
				for (int i = 0; i < set.getAtomContainerCount(); i++)
				{
					int numAtoms = set.getAtomContainer(i).getAtomCount();
					if (numAtoms > maxNumAtoms)
						maxNumAtoms = numAtoms;
					if (numAtoms < minNumAtoms)
					{
						minNumAtoms = numAtoms;
						minNumAtomsIndex = i;
					}
				}
				if (minNumAtoms <= 5 && maxNumAtoms >= 6)
				{
					if (DEBUG)
						System.err.println("removing ion/salt/acid "
								+ sg.createSMILES(set.getAtomContainer(minNumAtomsIndex)));
					set.removeAtomContainer(minNumAtomsIndex);
				}
				else
					break;
			}
			if (DEBUG && mixture.get(mol))
			{
				System.err.print("remaining : ");
				for (int i = 0; i < set.getAtomContainerCount(); i++)
					System.err.print((i > 0 ? "." : "") + sg.createSMILES(set.getAtomContainer(i)));
				System.err.println();
			}
			split.put(mol, set);
		}
		return split.get(mol);
	}

	public static String[] computeOBDescriptor(DatasetFile dataset, String id)
	{
		if (!hasMixture(dataset) || OBDescriptorFactory.getDescriptorDefaultType(id) != Type.NUMERIC)
			return OBDescriptorFactory.compute(dataset.getSDFPath(false), id);
		else
		{
			String sdfDestPath = Settings.destinationFile(dataset, dataset.getShortName() + "." + dataset.getMD5()
					+ ".resMixt.sdf");
			List<int[]> splitIndices = split(dataset, sdfDestPath);
			String vals[] = OBDescriptorFactory.compute(sdfDestPath, id);
			Double dVals[] = ArrayUtil.parseDoubleArray(vals);
			Double dRes[] = new Double[dataset.getCompounds().length];
			for (int i = 0; i < dRes.length; i++)
			{
				Double cVals[] = new Double[splitIndices.get(i).length];
				for (int j = 0; j < cVals.length; j++)
					cVals[j] = dVals[splitIndices.get(i)[j]];
				if (DEBUG)
					System.err.println("computing descriptor for mixture from "
							+ ArrayUtil.toString(splitIndices.get(i)) + ", instead of from " + i + " : "
							+ sg.createSMILES(dataset.getCompounds()[i]));
				dRes[i] = ArrayUtil.getMean(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(cVals)));
			}
			return ArrayUtil.toStringArray(dRes);
		}
	}

	private static boolean hasMixture(DatasetFile dataset)
	{
		boolean hasMixtures = false;
		for (IMolecule m : dataset.getCompounds())
			if (isMixture(m))
			{
				hasMixtures = true;
				break;
			}
		return hasMixtures;
	}

	private static HashMap<DatasetFile, List<int[]>> resolvedMixtureMap = new HashMap<DatasetFile, List<int[]>>();

	private static List<int[]> split(DatasetFile dataset, String sdfDest)
	{
		if (!resolvedMixtureMap.containsKey(dataset))
		{
			ArrayList<int[]> map = new ArrayList<int[]>();
			List<IAtomContainer> splitMols = new ArrayList<IAtomContainer>();
			for (int i = 0; i < dataset.getCompounds().length; i++)
			{
				IMoleculeSet set = split(dataset.getCompounds()[i]);
				map.add(new int[set.getAtomContainerCount()]);
				for (int j = 0; j < set.getAtomContainerCount(); j++)
				{
					map.get(i)[j] = splitMols.size();
					splitMols.add(set.getAtomContainer(j));
				}
				if (DEBUG)
					System.err.println("single compound " + i + " split to " + ArrayUtil.toString(map.get(i)));
			}
			if (DEBUG)
				System.err.println("single mixtures (" + splitMols.size() + " singles from "
						+ dataset.getCompounds().length + " mols) stored in " + sdfDest);
			try
			{
				SDFWriter writer = new SDFWriter(new FileOutputStream(sdfDest, false));
				for (IAtomContainer m : splitMols)
					writer.write(m);
				writer.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			resolvedMixtureMap.put(dataset, map);
		}
		return resolvedMixtureMap.get(dataset);
	}
}
