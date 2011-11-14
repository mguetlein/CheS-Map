package gui;

import gui.property.Property;
import gui.wizard.GenericWizardPanel;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import main.Settings;
import alg.Algorithm;
import alg.embed3d.AbstractRTo3DEmbedder;
import alg.embed3d.Random3DEmbedder;
import alg.embed3d.ThreeDEmbedder;
import alg.embed3d.WekaPCA3DEmbedder;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class EmbedWizardPanel extends GenericWizardPanel
{
	public static final ThreeDEmbedder EMBEDDERS[] = { new Random3DEmbedder(), new WekaPCA3DEmbedder(null),
			new AbstractRTo3DEmbedder.PCAFeature3DEmbedder(), new AbstractRTo3DEmbedder.TSNEFeature3DEmbedder(),
			new AbstractRTo3DEmbedder.SMACOF3DEmbedder() };

	JRadioButton embedButtons[];
	ButtonGroup group;

	ThreeDEmbedder embedder;
	boolean canProceed = false;

	public EmbedWizardPanel(CheSMapperWizard w)
	{
		super(w);
	}

	protected int defaultSelection()
	{
		return 1;
	}

	public ThreeDEmbedder get3DEmbedder()
	{
		return (ThreeDEmbedder) getSelectedAlgorithm();
	}

	@Override
	public String getTitle()
	{
		return Settings.text("embed.title");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("embed.desc");
	}

	@Override
	protected Algorithm[] getAlgorithms()
	{
		return EMBEDDERS;
	}

	@Override
	protected boolean hasSimpleView()
	{
		return true;
	}

	class SimpleEmbedPanel extends SimplePanel
	{
		JRadioButton buttonYes = new JRadioButton("Yes", true);
		JRadioButton buttonNo = new JRadioButton("No");

		public SimpleEmbedPanel()
		{
			ButtonGroup group = new ButtonGroup();
			group.add(buttonYes);
			group.add(buttonNo);
			ActionListener a = new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					updateSimpleSelection(e.getSource() == buttonYes);
				}
			};
			buttonYes.addActionListener(a);
			buttonNo.addActionListener(a);
			DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("p"));
			b.append(new JLabel("Embedd compounds according to their feature values into 3D space?"));
			b.nextLine();
			b.append(buttonYes);
			b.nextLine();
			b.append(buttonNo);

			setLayout(new BorderLayout());
			add(b.getPanel());

			String simpleSelected = (String) Settings.PROPS.get(getTitle() + "-simple-yes");
			if (simpleSelected != null && simpleSelected.equals("false"))
				buttonNo.setSelected(true);
		}

		private void updateSimpleSelection(boolean clusterYes)
		{
			wizard.update();
		}

		@Override
		protected Algorithm getAlgorithm()
		{
			if (buttonYes.isSelected())
				return getDefaultEmbedder();
			else
				return new Random3DEmbedder();
		}

		@Override
		protected void store()
		{
			Settings.PROPS.put(getTitle() + "-simple-yes", buttonYes.isSelected() ? "true" : "false");
		}
	}

	@Override
	protected SimplePanel createSimpleView()
	{
		return new SimpleEmbedPanel();
	}

	public static ThreeDEmbedder getDefaultEmbedder()
	{
		return new WekaPCA3DEmbedder(new Property[0]);
	}
}
