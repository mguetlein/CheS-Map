package data;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.color.CPKAtomColors;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.IGeneratorParameter;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.smiles.SmilesParser;

import util.SwingUtil;

public class CDKCompoundIcon
{
	public static ImageIcon createIcon(IMolecule iMolecule, boolean black) throws CDKException
	{
		Color fColor = black ? Color.WHITE : Color.BLACK;
		Color bColor = black ? Color.BLACK : Color.WHITE;
		int maxWidth = 120;
		int maxHeight = 160;

		int origWidth = 1;
		int origHeight = 1;
		Rectangle drawArea = new Rectangle(0, 0, origWidth, origHeight);
		StructureDiagramGenerator sdg = new StructureDiagramGenerator();
		sdg.setMolecule(iMolecule);
		sdg.generateCoordinates();
		iMolecule = sdg.getMolecule();
		List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
		generators.add(new BasicSceneGenerator());
		generators.add(new BasicBondGenerator());
		generators.add(new BasicAtomGenerator());
		AtomContainerRenderer renderer = new AtomContainerRenderer(generators, new AWTFontManager());
		CPKAtomColors cpkAtomColors = new CPKAtomColors();
		for (IGenerator<IAtomContainer> generator : renderer.getGenerators())
		{
			for (IGeneratorParameter<?> parameter : generator.getParameters())
			{
				if (parameter instanceof BasicSceneGenerator.BackgroundColor)
					((BasicSceneGenerator.BackgroundColor) parameter).setValue(bColor);
				else if (parameter instanceof BasicSceneGenerator.ForegroundColor)
					((BasicSceneGenerator.ForegroundColor) parameter).setValue(fColor);
				else if (parameter instanceof BasicBondGenerator.DefaultBondColor)
					((BasicBondGenerator.DefaultBondColor) parameter).setValue(fColor);
				else if (parameter instanceof BasicBondGenerator.BondLength)
					((BasicBondGenerator.BondLength) parameter).setValue(20.0);
				else if (parameter instanceof BasicBondGenerator.BondWidth)
					((BasicBondGenerator.BondWidth) parameter).setValue(2.0);
				else if (parameter instanceof BasicSceneGenerator.Margin)
					((BasicSceneGenerator.Margin) parameter).setValue(6.0);
				else if (parameter instanceof BasicAtomGenerator.AtomColorer)
					((BasicAtomGenerator.AtomColorer) parameter).setValue(cpkAtomColors);
			}
		}
		renderer.setup(iMolecule, drawArea);
		Rectangle diagramRectangle = renderer.calculateDiagramBounds(iMolecule);
		Rectangle result = renderer.shift(drawArea, diagramRectangle);
		origHeight = (int) (result.getHeight() + result.y);
		origWidth = (int) (result.getWidth() + result.x);
		Image image = new BufferedImage(origWidth, origHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = (Graphics2D) image.getGraphics();
		g2.setColor(bColor);
		g2.fillRect(0, 0, origWidth, origHeight);
		renderer.paint(iMolecule, new AWTDrawVisitor(g2));

		//scale
		double sx = maxWidth / (double) origWidth;
		double sy = maxHeight / (double) origHeight;
		double scale = Math.min(Math.min(sx, sy), 1);
		int scaledWidth = (int) (origWidth * scale);
		int scaledHeight = (int) (origHeight * scale);
		BufferedImage resizedImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = resizedImage.createGraphics();
		g.setComposite(AlphaComposite.Src);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
		g.dispose();

		return new ImageIcon(resizedImage);
	}

	public static void main(String args[]) throws CDKException
	{
		//IMolecule iMolecule = MoleculeFactory.make123Triazole();
		//String smiles = "BrN1C(=O)CCC1=O";
		String smiles = "C=C-Cl";
		SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
		IMolecule iMolecule = sp.parseSmiles(smiles);

		JPanel pB = new JPanel();
		pB.setBackground(Color.black);
//		pB.setBorder(new EmptyBorder(50, 50, 50, 50));
		pB.add(new JLabel(createIcon(iMolecule, true)));

		JPanel pW = new JPanel();
		pW.setBackground(Color.white);
//		pW.setBorder(new EmptyBorder(50, 50, 50, 50));
		pW.add(new JLabel(createIcon(iMolecule, false)));

		JPanel p = new JPanel();
		p.add(pB);
		p.add(pW);
		SwingUtil.showInDialog(p);
	}
}
