package org.chesmapper.map.data.desc;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.chesmapper.map.data.DatasetFile;
import org.chesmapper.map.dataInterface.CompoundPropertySet.Type;
import org.chesmapper.map.main.Settings;
import org.chesmapper.map.property.CDKDescriptor;
import org.chesmapper.map.property.OBDescriptorFactory;
import org.chesmapper.map.util.CDKUtil;
import org.mg.javalib.util.ArrayUtil;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.io.SDFWriter;

public class DescriptorForMixturesHandler
{
	private static boolean DEBUG = true;

	public static Double[] computeCDKDescriptor(CDKDescriptor desc, IAtomContainer mol)
	{
		if (!isMixture(mol))
			return desc.computeDescriptor(mol);
		else
		{
			IAtomContainerSet set = split(mol);
			List<Double[]> results = new ArrayList<Double[]>();
			if (DEBUG)
			{
				String msg = "computing descriptor for mixture from ";
				for (int i = 0; i < set.getAtomContainerCount(); i++)
					msg += CDKUtil.createSmiles(set.getAtomContainer(i)) + " ";
				msg += "orig compound was " + CDKUtil.createSmiles(mol);
				Settings.LOGGER.debug(msg);
			}
			for (int i = 0; i < set.getAtomContainerCount(); i++)
				results.add(desc.computeDescriptor(set.getAtomContainer(i)));
			return ArrayUtil.computeMean(results);
		}
	}

	public static boolean isMixture(IAtomContainer mol)
	{
		split(mol);
		return mixture.get(mol);
	}

	private static HashMap<IAtomContainer, IAtomContainerSet> split = new HashMap<IAtomContainer, IAtomContainerSet>();
	private static HashMap<IAtomContainer, Boolean> mixture = new HashMap<IAtomContainer, Boolean>();

	private static IAtomContainerSet split(IAtomContainer mol)
	{
		if (!split.containsKey(mol))
		{
			IAtomContainerSet set = ConnectivityChecker.partitionIntoMolecules(mol);
			if (set.getAtomContainerCount() > 1)
			{
				mixture.put(mol, true);
				if (DEBUG)
					Settings.LOGGER.debug("Mixture found: " + set.getAtomContainerCount() + " : "
							+ CDKUtil.createSmiles(mol));
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
						Settings.LOGGER.debug("removing ion/salt/acid "
								+ CDKUtil.createSmiles(set.getAtomContainer(minNumAtomsIndex)));
					set.removeAtomContainer(minNumAtomsIndex);
				}
				else
					break;
			}
			if (DEBUG && mixture.get(mol))
			{
				String s = "remaining : ";
				for (int i = 0; i < set.getAtomContainerCount(); i++)
					s += (i > 0 ? "." : "") + CDKUtil.createSmiles(set.getAtomContainer(i));
				Settings.LOGGER.debug(s);
			}
			split.put(mol, set);
		}
		return split.get(mol);
	}

	public static String[] computeOBDescriptor(DatasetFile dataset, String id)
	{
		if (!hasMixture(dataset) || OBDescriptorFactory.getDescriptorDefaultType(id) != Type.NUMERIC)
			return OBDescriptorFactory.compute(dataset.getSDF(), id);
		else
		{
			String sdfDestPath = Settings.destinationFile(dataset, "resMixt.sdf");
			List<int[]> splitIndices = split(dataset, sdfDestPath);
			String vals[] = OBDescriptorFactory.compute(sdfDestPath, id);
			Double dVals[] = ArrayUtil.parseDoubleArray(vals);
			Double dRes[] = new Double[dataset.getCompounds().length];
			for (int i = 0; i < dRes.length; i++)
			{
				Double cVals[] = new Double[splitIndices.get(i).length];
				for (int j = 0; j < cVals.length; j++)
					cVals[j] = dVals[splitIndices.get(i)[j]];
				if (DEBUG && isMixture(dataset.getCompounds()[i]))
					Settings.LOGGER.debug("computing descriptor for mixture from "
							+ ArrayUtil.toString(splitIndices.get(i)) + "orig compound was " + i + " : "
							+ CDKUtil.createSmiles(dataset.getCompounds()[i]));
				dRes[i] = ArrayUtil.getMean(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(cVals)));
			}
			return ArrayUtil.toStringArray(dRes);
		}
	}

	private static boolean hasMixture(DatasetFile dataset)
	{
		boolean hasMixtures = false;
		for (IAtomContainer m : dataset.getCompounds())
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
				IAtomContainerSet set = split(dataset.getCompounds()[i]);
				map.add(new int[set.getAtomContainerCount()]);
				for (int j = 0; j < set.getAtomContainerCount(); j++)
				{
					map.get(i)[j] = splitMols.size();
					splitMols.add(set.getAtomContainer(j));
				}
				if (DEBUG && set.getAtomContainerCount() > 1)
					Settings.LOGGER.debug("single compound " + i + " split to " + ArrayUtil.toString(map.get(i)));
			}
			if (DEBUG)
				Settings.LOGGER.debug("single mixtures (" + splitMols.size() + " singles from "
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
