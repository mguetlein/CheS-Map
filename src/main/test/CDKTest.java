package main.test;

import main.Settings;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.BCUTDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.WeightDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.WienerNumbersDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.XLogPDescriptor;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

public class CDKTest
{
	public static void main(String args[])
	{
		try
		{
			SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
			IMolecule m = sp.parseSmiles("N[C@@H](C)C(=O)O");

			CDKHydrogenAdder ha = CDKHydrogenAdder.getInstance(DefaultChemObjectBuilder.getInstance());
			AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(m);
			ha.addImplicitHydrogens(m);
			AtomContainerManipulator.convertImplicitToExplicitHydrogens(m);

			Settings.LOGGER.info(m);

			IMolecularDescriptor x = new XLogPDescriptor();
			Settings.LOGGER.info("xlogp " + x.calculate(m).getValue());

			x = new WeightDescriptor();
			Settings.LOGGER.info("weight " + x.calculate(m).getValue());

			x = new WienerNumbersDescriptor();
			Settings.LOGGER.info("wiener " + x.calculate(m).getValue());

			x = new BCUTDescriptor();
			Settings.LOGGER.info("bcut " + x.calculate(m).getValue());
		}
		catch (InvalidSmilesException e)
		{
			Settings.LOGGER.error(e);
		}
		catch (CDKException e)
		{
			Settings.LOGGER.error(e);
		}

	}
}
