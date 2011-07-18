package main.test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import javax.vecmath.Point3d;

import org.openscience.cdk.Atom;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.geometry.alignment.KabschAlignment;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.isomorphism.mcss.RMap;

import util.ArrayUtil;
import data.CDKProperty;
import data.CDKService;

public class AlginTest
{
	public static void main(String args[])
	{
		String backbone = "/home/martin/tmp/align_test/backbones1.0.sdf";
		String cluster = "/home/martin/tmp/align_test/cluster1.0.ob3d.sdf";

		//		OpenBabel3DGenerator threed = new OpenBabel3DGenerator(); 
		//		threed.generate3D(backbone);
		//		threed.generate3D(cluster);

		IMolecule mols[] = CDKService.getMoleculeFromSdf(cluster, false);
		System.out.println(ArrayUtil.toString(CDKService.getStringFromSdf(cluster, CDKProperty.SMILES)));
		for (IMolecule iMolecule : mols)
		{
			System.out.println(iMolecule.getAtomCount());
		}

		IMolecule back[] = CDKService.getMoleculeFromSdf(backbone, false);
		System.out.println(ArrayUtil.toString(CDKService.getStringFromSdf(backbone, CDKProperty.SMILES)));
		for (IMolecule iMolecule : back)
		{
			System.out.println(iMolecule.getAtomCount());
		}

		System.out.println();

		try
		{
			//			SDFWriter w = new SDFWriter(new FileWriter(backbone + ".algined"));
			//			w.write(back[0]);
			//			w.close();

			List<RMap> map = UniversalIsomorphismTester.getSubgraphAtomsMap(mols[0], back[0]);
			List<RMap> map2 = UniversalIsomorphismTester.getSubgraphAtomsMap(mols[1], back[0]);

			Atom atoms[] = new Atom[map.size()];
			Atom atoms2[] = new Atom[map2.size()];

			for (int i = 0; i < atoms.length; i++)
			{
				atoms[i] = (Atom) mols[0].getAtom(map.get(i).getId1());
				atoms2[i] = (Atom) mols[1].getAtom(map2.get(i).getId1());
			}

			KabschAlignment align = new KabschAlignment(atoms, atoms2);
			align.align();
			System.out.println(align.getRMSD());

			SDFWriter w = new SDFWriter(new FileWriter(cluster + ".algined"));

			Point3d cm1 = align.getCenterOfMass();
			for (int i = 0; i < mols[0].getAtomCount(); i++)
			{
				Atom a = (Atom) mols[0].getAtom(i);
				a.setPoint3d(new Point3d(a.getPoint3d().x - cm1.x, a.getPoint3d().y - cm1.y, a.getPoint3d().z - cm1.z));
			}
			align.rotateAtomContainer(mols[1]);

			w.write(mols[0]);
			w.write(mols[1]);
			w.close();
		}
		catch (CDKException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//		KabschAlignment align = new KabschAlignment(al1, al2);
	}
}
